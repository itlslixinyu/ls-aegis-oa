"""从标准输入的二维JSON生成合成XLSX，供隔离业务上传测试使用。"""
import io
import json
import sys
import zipfile
from xml.sax.saxutils import escape


def main():
    rows = json.load(sys.stdin)
    if not isinstance(rows, list) or len(rows) > 1000:
        raise ValueError('夹具仅接受最多1000行的二维数组')
    cells = []
    for row_number, row in enumerate(rows, 1):
        if not isinstance(row, list) or len(row) > 26:
            raise ValueError('夹具每行最多26列')
        values = ''.join(f'<c r="{chr(65 + col)}{row_number}" t="inlineStr"><is><t xml:space="preserve">{escape(str(value))}</t></is></c>'
                         for col, value in enumerate(row))
        cells.append(f'<row r="{row_number}">{values}</row>')
    output = io.BytesIO()
    with zipfile.ZipFile(output, 'w', zipfile.ZIP_DEFLATED) as archive:
        archive.writestr('[Content_Types].xml', '<?xml version="1.0"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>')
        archive.writestr('_rels/.rels', '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>')
        archive.writestr('xl/workbook.xml', '<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="导入数据" sheetId="1" r:id="rId1"/></sheets></workbook>')
        archive.writestr('xl/_rels/workbook.xml.rels', '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>')
        archive.writestr('xl/worksheets/sheet1.xml', '<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>' + ''.join(cells) + '</sheetData></worksheet>')
    sys.stdout.buffer.write(output.getvalue())


if __name__ == '__main__':
    main()
