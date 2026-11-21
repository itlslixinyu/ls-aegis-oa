package com.ls.aegis.biz.sign.chain;

import com.ls.aegis.biz.sign.domain.vo.GeneralSealConfig;
import com.ls.aegis.biz.sign.domain.vo.SignResult;
import com.ls.aegis.biz.sign.domain.vo.SpanSealConfig;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SignChainContext {

    private byte[] sourcePdf;
    private int pageCount;
    private float pageWidth;
    private float pageHeight;

    private List<GeneralSealConfig> generalSeals = new ArrayList<>();
    private List<SpanSealConfig> spanSeals = new ArrayList<>();

    private Map<Long, BufferedImage> sealImageCache = new HashMap<>();

    private byte[] renderedPdf;

    private byte[] finalPdf;
    private SignResult signResult;

    private String chainStatus = "pending";
    private int totalTasks;
    private int completedTasks;
    private String errorMessage;

    private Long primarySealId;
    private Long skipSealId;
    private Boolean useManagedCert;

    public byte[] getSourcePdf() {
        return sourcePdf;
    }

    public void setSourcePdf(byte[] sourcePdf) {
        this.sourcePdf = sourcePdf;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public float getPageWidth() {
        return pageWidth;
    }

    public void setPageWidth(float pageWidth) {
        this.pageWidth = pageWidth;
    }

    public float getPageHeight() {
        return pageHeight;
    }

    public void setPageHeight(float pageHeight) {
        this.pageHeight = pageHeight;
    }

    public List<GeneralSealConfig> getGeneralSeals() {
        return generalSeals;
    }

    public void setGeneralSeals(List<GeneralSealConfig> generalSeals) {
        this.generalSeals = generalSeals;
    }

    public List<SpanSealConfig> getSpanSeals() {
        return spanSeals;
    }

    public void setSpanSeals(List<SpanSealConfig> spanSeals) {
        this.spanSeals = spanSeals;
    }

    public Map<Long, BufferedImage> getSealImageCache() {
        return sealImageCache;
    }

    public void setSealImageCache(Map<Long, BufferedImage> sealImageCache) {
        this.sealImageCache = sealImageCache;
    }

    public byte[] getRenderedPdf() {
        return renderedPdf;
    }

    public void setRenderedPdf(byte[] renderedPdf) {
        this.renderedPdf = renderedPdf;
    }

    public byte[] getFinalPdf() {
        return finalPdf;
    }

    public void setFinalPdf(byte[] finalPdf) {
        this.finalPdf = finalPdf;
    }

    public SignResult getSignResult() {
        return signResult;
    }

    public void setSignResult(SignResult signResult) {
        this.signResult = signResult;
    }

    public String getChainStatus() {
        return chainStatus;
    }

    public void setChainStatus(String chainStatus) {
        this.chainStatus = chainStatus;
    }

    public int getTotalTasks() {
        return totalTasks;
    }

    public void setTotalTasks(int totalTasks) {
        this.totalTasks = totalTasks;
    }

    public int getCompletedTasks() {
        return completedTasks;
    }

    public void setCompletedTasks(int completedTasks) {
        this.completedTasks = completedTasks;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getPrimarySealId() {
        return primarySealId;
    }

    public void setPrimarySealId(Long primarySealId) {
        this.primarySealId = primarySealId;
    }

    public Long getSkipSealId() {
        return skipSealId;
    }

    public void setSkipSealId(Long skipSealId) {
        this.skipSealId = skipSealId;
    }

    public Boolean getUseManagedCert() {
        return useManagedCert;
    }

    public void setUseManagedCert(Boolean useManagedCert) {
        this.useManagedCert = useManagedCert;
    }
}
