package com.example.demo.lagou.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

import java.util.HashMap;
import java.util.Map;

public class PoiUtils {
    /**
     * 根据行内容重新计算行高
     */
    public static void calcAndSetRowHeigt(XSSFRow sourceRow) {
        for (int cellIndex = sourceRow.getFirstCellNum(); cellIndex <= sourceRow.getPhysicalNumberOfCells(); cellIndex++) {
            //行高
            double maxHeight = sourceRow.getHeight();
            XSSFCell sourceCell = sourceRow.getCell(cellIndex);
            //单元格的内容
            String cellContent = getCellContentAsString(sourceCell);
            if(null == cellContent || "".equals(cellContent)){
                continue;
            }
            //单元格的宽高及单元格信息
            Map<String, Object> cellInfoMap = getCellInfo(sourceCell);
            Integer cellWidth = (Integer)cellInfoMap.get("width");
            Integer cellHeight = (Integer)cellInfoMap.get("height");
            if(cellHeight > maxHeight){
                maxHeight = cellHeight;
            }
            XSSFCellStyle cellStyle = sourceCell.getCellStyle();
            XSSFFont font = cellStyle.getFont();
            //字体的高度
            short fontHeight = font.getFontHeight();

            //cell内容字符串总宽度
            double cellContentWidth = cellContent.getBytes().length * 2 * 256;

            //字符串需要的行数 不做四舍五入之类的操作
            double stringNeedsRows =(double)cellContentWidth / cellWidth;
            //小于一行补足一行
            if(stringNeedsRows < 1.0){
                stringNeedsRows = 1.0;
            }

            //需要的高度             (Math.floor(stringNeedsRows) - 1) * 40 为两行之间空白高度
            double stringNeedsHeight = (double)fontHeight * stringNeedsRows;
            //需要重设行高
            if(stringNeedsHeight > maxHeight){
                maxHeight = stringNeedsHeight;
                //超过原行高三倍 则为5倍 实际应用中可做参数配置
                if(maxHeight/cellHeight > 5){
                    maxHeight = 5 * cellHeight;
                }
                //最后取天花板防止高度不够
                maxHeight = Math.ceil(maxHeight);
                //重新设置行高 同时处理多行合并单元格的情况
                Boolean isPartOfRowsRegion = (Boolean)cellInfoMap.get("isPartOfRowsRegion");
                if(isPartOfRowsRegion){
                    Integer firstRow = (Integer)cellInfoMap.get("firstRow");
                    Integer lastRow = (Integer)cellInfoMap.get("lastRow");
                    //平均每行需要增加的行高
                    double addHeight = (maxHeight - cellHeight)/(lastRow - firstRow + 1);
                    for (int i = firstRow; i <= lastRow; i++) {
                        double rowsRegionHeight =sourceRow.getSheet().getRow(i).getHeight() + addHeight;
                        sourceRow.getSheet().getRow(i).setHeight((short)rowsRegionHeight);
                    }
                }else{
                    sourceRow.setHeight((short)maxHeight);
                }
            }
        }
    }

    /**
     * 解析一个单元格得到数据
     * @param cell
     * @return
     */
    private static String getCellContentAsString(XSSFCell cell) {
        if(null == cell){
            return "";
        }
        String result = "";
        switch (cell.getCellType()) {
            case Cell.CELL_TYPE_NUMERIC:
                String s = String.valueOf(cell.getNumericCellValue());
                if (s != null) {
                    if (s.endsWith(".0")) {
                        s = s.substring(0, s.length() - 2);
                    }
                }
                result = s;
                break;
            case Cell.CELL_TYPE_STRING:
                String stringCellValue = cell.getStringCellValue();
                result = StringUtils.isNotBlank(stringCellValue)?stringCellValue:"";
                break;
            case Cell.CELL_TYPE_BLANK:
                break;
            case Cell.CELL_TYPE_BOOLEAN:
                result = String.valueOf(cell.getBooleanCellValue());
                break;
            case Cell.CELL_TYPE_ERROR:
                break;
            default:
                break;
        }
        return result;
    }

    /**
     * 获取单元格及合并单元格的宽度
     * @param cell
     * @return
     */
    private static Map<String, Object> getCellInfo(XSSFCell cell) {
        XSSFSheet sheet = cell.getSheet();
        int rowIndex = cell.getRowIndex();
        int columnIndex = cell.getColumnIndex();

        boolean isPartOfRegion = false;
        int firstColumn = 0;
        int lastColumn = 0;
        int firstRow = 0;
        int lastRow = 0;
        int sheetMergeCount = sheet.getNumMergedRegions();
        for (int i = 0; i < sheetMergeCount; i++) {
            CellRangeAddress ca = sheet.getMergedRegion(i);
            firstColumn = ca.getFirstColumn();
            lastColumn = ca.getLastColumn();
            firstRow = ca.getFirstRow();
            lastRow = ca.getLastRow();
            if (rowIndex >= firstRow && rowIndex <= lastRow) {
                if (columnIndex >= firstColumn && columnIndex <= lastColumn) {
                    isPartOfRegion = true;
                    break;
                }
            }
        }
        Map<String, Object> map = new HashMap<String, Object>();
        Integer width = 0;
        Integer height = 0;
        boolean isPartOfRowsRegion = false;
        if(isPartOfRegion){
            for (int i = firstColumn; i <= lastColumn; i++) {
                width += sheet.getColumnWidth(i);
            }
            for (int i = firstRow; i <= lastRow; i++) {
                height += sheet.getRow(i).getHeight();
            }
            if(lastRow > firstRow){
                isPartOfRowsRegion = true;
            }
        }else{
            width = sheet.getColumnWidth(columnIndex);
            height += cell.getRow().getHeight();
        }
        map.put("isPartOfRowsRegion", isPartOfRowsRegion);
        map.put("firstRow", firstRow);
        map.put("lastRow", lastRow);
        map.put("width", width);
        map.put("height", height);
        return map;
    }
}
