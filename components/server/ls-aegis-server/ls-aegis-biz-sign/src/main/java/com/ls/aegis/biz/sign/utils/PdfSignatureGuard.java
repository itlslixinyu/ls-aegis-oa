package com.ls.aegis.biz.sign.utils;

import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.signatures.SignatureUtil;
import com.ls.aegis.biz.sign.domain.vo.PdfSignatureReport;
import com.ls.aegis.biz.sign.domain.vo.SignaturePreservationResult;
import com.ls.aegis.compat.common.exception.ServiceException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.Security;
import java.util.*;

/** Fail closed before persistence if a continuation loses or changes any existing signature. */
public final class PdfSignatureGuard {
    public static final String SCOPE = "SIGNATURE_INTEGRITY_ONLY";
    private static final int MAX_BYTES = 64 * 1024 * 1024;
    private static final int MAX_SIGNATURES = 64;

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private PdfSignatureGuard() { }

    public record Baseline(byte[] source, PdfSignatureReport report, Map<String, String> signatureDigests) { }

    public static PdfSignatureReport inspect(byte[] bytes) {
        return capture(bytes).report();
    }

    public static Baseline beforeSigning(byte[] bytes) {
        Baseline baseline = capture(bytes);
        if (!baseline.report().continuationAllowed()) {
            throw new ServiceException(baseline.report().message());
        }
        return baseline;
    }

    /** A signed input must never pass through a full rewrite, including intermediate render stages. */
    public static StampingProperties stampingProperties(byte[] bytes) {
        try (PdfDocument document = open(bytes)) {
            return new SignatureUtil(document).getSignatureNames().isEmpty()
                ? new StampingProperties() : new StampingProperties().useAppendMode();
        } catch (Exception e) {
            throw new ServiceException("无法读取PDF签名结构，请使用完整的电子原件");
        }
    }

    public static SignaturePreservationResult afterSigning(Baseline before, byte[] output, boolean requireNewSignature) {
        Baseline after = capture(output);
        int countBefore = before.report().signatureCount();
        int countAfter = after.report().signatureCount();
        if (countBefore > 0) {
            if (output.length <= before.source().length) fail();
            for (int i = 0; i < before.source().length; i++) {
                if (output[i] != before.source()[i]) fail();
            }
            for (var entry : before.signatureDigests().entrySet()) {
                if (!entry.getValue().equals(after.signatureDigests().get(entry.getKey()))) fail();
            }
        }
        if ((requireNewSignature || countBefore > 0) && countAfter <= countBefore) {
            throw new ServiceException("未生成新的数字签名，已停止保存；请检查印章证书后重试");
        }
        if (countAfter > 0 && (!after.report().allSignaturesIntact()
            || !after.report().latestSignatureCoversWholeDocument())) fail();
        return new SignaturePreservationResult(countBefore, countAfter, countAfter - countBefore,
            true, countAfter > 0 && after.report().allSignaturesIntact(), SCOPE);
    }

    private static void fail() {
        throw new ServiceException("签署后验签失败：已有签名被修改、丢失或签名完整性异常，已停止保存");
    }

    private static PdfDocument open(byte[] bytes) throws Exception {
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_BYTES) {
            throw new ServiceException("PDF为空或超过64MB签名检查上限");
        }
        return new PdfDocument(new PdfReader(new ByteArrayInputStream(bytes)));
    }

    private static Baseline capture(byte[] bytes) {
        try (PdfDocument document = open(bytes)) {
            SignatureUtil util = new SignatureUtil(document);
            List<String> names = util.getSignatureNames();
            if (names.size() > MAX_SIGNATURES) throw new ServiceException("PDF签名数量超过64个，暂不支持继续签署");
            var entries = new ArrayList<PdfSignatureReport.Entry>();
            var digests = new LinkedHashMap<String, String>();
            boolean restricted = hasRestrictions(document, util, names);
            for (String name : names) {
                boolean intact = false;
                String signer = "";
                String signedAt = "";
                try {
                    var signature = util.readSignatureData(name);
                    intact = signature != null && signature.verifySignatureIntegrityAndAuthenticity();
                    if (signature != null) {
                        signer = Objects.toString(signature.getSignName(), "");
                        if (signer.isBlank() && signature.getSigningCertificate() != null) {
                            signer = signature.getSigningCertificate().getSubjectX500Principal().getName();
                        }
                        signedAt = signature.getSignDate() == null ? "" : signature.getSignDate().toInstant().toString();
                    }
                    PdfDictionary dictionary = util.getSignatureDictionary(name);
                    PdfString contents = dictionary.getAsString(PdfName.Contents);
                    PdfArray range = dictionary.getAsArray(PdfName.ByteRange);
                    if (contents == null || range == null) intact = false;
                    else digests.put(name, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                        .digest(contents.getValueBytes())) + ":" + range);
                } catch (Exception ignored) {
                    // A malformed/unsupported signature must never be silently ignored during continuation.
                }
                entries.add(new PdfSignatureReport.Entry(name, intact, util.signatureCoversWholeDocument(name),
                    util.getRevision(name), signer, signedAt));
            }
            PdfAcroForm form = PdfAcroForm.getAcroForm(document, false);
            if (form != null) for (var field : form.getAllFormFields().entrySet()) {
                if (PdfName.Sig.equals(field.getValue().getFormType())
                    && field.getValue().getPdfObject().get(PdfName.V) != null
                    && !field.getValue().getPdfObject().get(PdfName.V).isNull() && !names.contains(field.getKey())) {
                    entries.add(new PdfSignatureReport.Entry(field.getKey(), false, false, 0, "", ""));
                }
            }
            int count = entries.size();
            boolean intact = count > 0 && entries.stream().allMatch(PdfSignatureReport.Entry::intact);
            boolean latestCovers = !names.isEmpty() && util.signatureCoversWholeDocument(names.get(names.size() - 1));
            boolean allowed = !restricted && (count == 0 || (intact && latestCovers));
            String message = restricted ? "文件含认证签名或字段锁定限制，当前流程不能安全续签；请使用允许续签的电子原件"
                : count == 0 ? "未检测到数字签名；若文件曾经签署，请确认没有经过扫描或转换"
                : !intact ? "已有签名完整性校验未通过，不能继续签署；请提供未被修改的已签电子原件"
                : !latestCovers ? "最后一次签名后存在未签署的修改，不能自动续签；请核对电子原件"
                : "已有" + count + "个数字签名，完整性校验通过，将以追加方式续签";
            return new Baseline(bytes, new PdfSignatureReport(count, intact, latestCovers, allowed,
                message, SCOPE, List.copyOf(entries)), Map.copyOf(digests));
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("PDF签名解析失败，请使用完整且可打开的电子原件");
        }
    }

    private static boolean hasRestrictions(PdfDocument document, SignatureUtil util, List<String> names) {
        PdfDictionary permissions = document.getCatalog().getPdfObject().getAsDictionary(PdfName.Perms);
        if (permissions != null && permissions.containsKey(PdfName.DocMDP)) return true;
        for (String name : names) {
            PdfArray references = util.getSignatureDictionary(name).getAsArray(PdfName.Reference);
            if (references == null) continue;
            for (int i = 0; i < references.size(); i++) {
                PdfDictionary reference = references.getAsDictionary(i);
                if (reference == null) return true;
                PdfName method = reference.getAsName(PdfName.TransformMethod);
                if (PdfName.DocMDP.equals(method) || PdfName.FieldMDP.equals(method)) return true;
            }
        }
        PdfAcroForm form = PdfAcroForm.getAcroForm(document, false);
        if (form != null && !names.isEmpty()) {
            return form.getAllFormFields().values().stream()
                .anyMatch(field -> field.getPdfObject().containsKey(PdfName.Lock));
        }
        return false;
    }
}
