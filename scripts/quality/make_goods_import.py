"""Populate one synthetic row in the actual downloaded goods import template."""
import argparse
import zipfile
import xml.etree.ElementTree as ET

p=argparse.ArgumentParser();p.add_argument('template');p.add_argument('output');p.add_argument('code');p.add_argument('name');p.add_argument('category');p.add_argument('--price',default='80');a=p.parse_args()
ns='http://schemas.openxmlformats.org/spreadsheetml/2006/main'
ET.register_namespace('',ns)
with zipfile.ZipFile(a.template) as source:
    sheet=ET.fromstring(source.read('xl/worksheets/sheet1.xml'))
    dimension=sheet.find('{%s}dimension'%ns)
    if dimension is not None:dimension.set('ref','A1:M2')
    data=sheet.find('{%s}sheetData'%ns)
    for row in list(data):
        if int(row.get('r','0'))>1:data.remove(row)
    row=ET.SubElement(data,'{%s}row'%ns,{'r':'2'})
    values=[a.code,a.name,a.category,'SYNTHETIC','件','0','','50',a.price,'0','100','1','ITLS TEST ONLY']
    for i,value in enumerate(values):
        c=ET.SubElement(row,'{%s}c'%ns,{'r':chr(65+i)+'2','t':'inlineStr'})
        ET.SubElement(ET.SubElement(c,'{%s}is'%ns),'{%s}t'%ns).text=value
    with zipfile.ZipFile(a.output,'w',zipfile.ZIP_DEFLATED) as out:
        for item in source.infolist():out.writestr(item,ET.tostring(sheet,encoding='utf-8',xml_declaration=True) if item.filename=='xl/worksheets/sheet1.xml' else source.read(item.filename))
