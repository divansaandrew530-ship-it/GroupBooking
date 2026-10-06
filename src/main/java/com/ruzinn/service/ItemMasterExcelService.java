package com.ruzinn.service;

import com.ruzinn.dto.ItemMasterForm;
import java.io.InputStream;
import java.util.List;

public interface ItemMasterExcelService {

  byte[] template();

  List<ParsedRow> parse(InputStream input);

  record ParsedRow(int excelRow, ItemMasterForm form) {}
}
