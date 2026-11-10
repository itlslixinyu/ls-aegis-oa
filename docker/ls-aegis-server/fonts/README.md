# 中文字体（PDF 导出 + 水印）

将 **TrueType** 中文字体放到本目录，文件名：`simhei.ttf`。

- 可用 Windows 的 `C:\Windows\Fonts\simhei.ttf` 复制过来
- 不要用 Noto CJK（OTF/CFF，会报 `OTF fonts do not have a glyf table`）
- 本目录字体默认不入库（见 `.gitignore`），部署机需自行放置

Compose 已挂载到容器 `/app/fonts/`：

- PDF 导出：`QuotePdfExporter` 读 `/app/fonts/simhei.ttf`
- 图片水印：`WatermarkUtils` 同样优先读该路径；镜像内另有 `fonts-wqy-microhei` 兜底
