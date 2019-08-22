package com.example.demo.lagou.easypoi;

import cn.afterturn.easypoi.handler.impl.ExcelDataHandlerDefaultImpl;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutputJobHandler extends ExcelDataHandlerDefaultImpl<OutputJob> {
    private static final Logger log = LoggerFactory.getLogger(OutputJobHandler.class);

    @Override
    public Object exportHandler(OutputJob obj, String name, Object value) {
        log.info(name+":"+value);
        return super.exportHandler(obj, name, value);
    }

    @Override
    public Hyperlink getHyperlink(CreationHelper creationHelper, OutputJob obj, String name, Object value) {
        Hyperlink hyperlink = creationHelper.createHyperlink(Hyperlink.LINK_URL);
        hyperlink.setAddress(String.valueOf(value));
        return hyperlink;
    }
}
