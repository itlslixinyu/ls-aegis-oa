<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8"/>
    <meta name="description" content="邮箱验证码"/>
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no"/>
    <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
    <title>${siteTitle!''} 邮箱验证码</title>
</head>
<body style="margin:0;padding:0;background:#e8eef8;font-family:'Microsoft YaHei',SimHei,Arial,sans-serif;">
<div style="padding:24px 12px 40px;background:#e8eef8;">
    <div style="max-width:560px;margin:0 auto;background:#ffffff;border-radius:14px;overflow:hidden;box-shadow:0 12px 32px rgba(8,40,110,0.14);">
        <!-- 顶栏：标题与公司名相对蓝底左右居中 -->
        <div style="background:#1e6fff;padding:20px 24px;text-align:center;">
            <div style="color:#ffffff;font-size:22px;font-weight:800;letter-spacing:0.02em;line-height:1.35;">
                <a href="${siteUrl!''}" style="color:#ffffff;text-decoration:none;font-weight:800;">${siteTitle!''}</a>
            </div>
            <#if (siteCompany!'')?has_content>
            <div style="color:rgba(255,255,255,0.92);font-size:15px;font-weight:600;margin-top:6px;line-height:1.3;">${siteCompany}</div>
            </#if>
        </div>

        <!-- 正文 -->
        <div style="padding:28px 28px 8px;color:#1d2129;">
            <!-- 中文（验证码上方） -->
            <div style="font-size:18px;font-weight:700;color:#163a8a;margin-bottom:12px;">亲爱的用户：</div>
            <p style="margin:0 0 18px;font-size:14px;line-height:1.75;color:#4e5969;">
                您好！感谢使用 <span style="color:#1e6fff;font-weight:600;">${siteTitle!''}</span>。
                您正在进行身份验证，请使用下方验证码完成操作。
            </p>

            <!-- 英文（验证码上方） -->
            <div style="font-size:16px;font-weight:700;color:#163a8a;margin:0 0 10px;">Dear User,</div>
            <p style="margin:0 0 18px;font-size:13px;line-height:1.75;color:#4e5969;">
                Thank you for using <span style="color:#1e6fff;font-weight:600;">${siteTitle!''}</span>.
                Please use the verification code below to complete authentication.
            </p>

            <!-- 验证码卡片 -->
            <div style="margin:4px 0 22px;padding:22px 16px;text-align:center;background:#f5f9ff;border:1px solid #d6e8ff;border-radius:12px;">
                <div style="font-size:12px;color:#86909c;letter-spacing:0.08em;margin-bottom:6px;">邮箱验证码 / Verification Code</div>
                <div style="font-size:36px;font-weight:700;letter-spacing:0.28em;color:#1e6fff;font-variant-numeric:tabular-nums;">${captcha!''}</div>
                <div style="margin-top:12px;font-size:13px;color:#86909c;">
                    请在 <span style="color:#f53f3f;font-weight:600;">${expiration!''}</span> 分钟内使用
                    <span style="color:#c9cdd4;"> · </span>
                    Valid for <span style="color:#f53f3f;font-weight:600;">${expiration!''}</span> minutes
                </div>
            </div>

            <div style="margin:8px 0 8px;padding-top:14px;border-top:1px solid #e5e6eb;">
                <p style="margin:0 0 6px;font-size:12px;line-height:1.7;color:#86909c;">
                    若非本人操作，请忽略此邮件。本邮件由系统自动发送，请勿直接回复。
                </p>
                <p style="margin:0;font-size:12px;line-height:1.7;color:#c9cdd4;">
                    If you did not request this, please ignore this email. This message was sent automatically; do not reply.
                </p>
            </div>
        </div>

        <!-- 页脚 -->
        <div style="padding:16px 28px 22px;border-top:1px solid #e5e6eb;">
            <div style="font-size:12px;line-height:1.7;color:#c9cdd4;text-align:center;">
                ${siteCopyright!''}
            </div>
        </div>
    </div>
</div>
</body>
</html>
