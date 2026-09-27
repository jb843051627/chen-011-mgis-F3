package com.fc.v2.common.submit;

/**
 * 整册顶回：行数越过单册上限、一张白纸（零行）之类，
 * 一律当场顶回，册头、行、总账一笔都不落，也不给收讫回执。
 *
 * @author fuce
 * @date 2026-09-26
 */
public class CatalogAcceptException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CatalogAcceptException(String message) {
        super(message);
    }
}
