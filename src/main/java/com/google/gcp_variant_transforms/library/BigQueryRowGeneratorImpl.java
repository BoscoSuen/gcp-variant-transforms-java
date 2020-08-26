// Copyright 2020 Google LLC

package com.google.gcp_variant_transforms.library;

import com.google.api.services.bigquery.model.TableRow;
import com.google.gcp_variant_transforms.common.Constants;
import com.google.inject.Inject;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFHeader;
import java.io.Serializable;
import java.util.List;

/**
 * Implementation class for BigQuery row generator. The class provides functionality to generate
 * a {@link TableRow} and set table row field value from {@link VariantContext}. Each value in
 * the table field should match the format in {@link VCFHeader}.
 */
public class BigQueryRowGeneratorImpl implements BigQueryRowGenerator, Serializable {

  @Inject
  VariantToBqUtils variantToBqUtils;

  public TableRow convertToBQRow(VariantContext variantContext, VCFHeader vcfHeader) {
    TableRow row = new TableRow();

    row.set(Constants.ColumnKeyConstants.REFERENCE_NAME, variantContext.getContig());
    row.set(Constants.ColumnKeyConstants.START_POSITION, variantContext.getStart());
    row.set(Constants.ColumnKeyConstants.END_POSITION, variantContext.getEnd());
    try {
      row.set(Constants.ColumnKeyConstants.REFERENCE_BASES,
              variantToBqUtils.getReferenceBases(variantContext));
      row.set(Constants.ColumnKeyConstants.NAMES, variantToBqUtils.getNames(variantContext));

      // Write alt field and info field to BQ row.
      List<TableRow> altMetadata = variantToBqUtils.getAlternateBases(variantContext);
      // AltMetadata should contain Info fields with Number='A' tag, then be added to the row.
      // The rest of Info fields will be directly added to the base BQ row.
      variantToBqUtils.addInfo(row, variantContext, altMetadata, vcfHeader, altMetadata.size());
      row.set(Constants.ColumnKeyConstants.ALTERNATE_BASES, altMetadata);

      row.set(Constants.ColumnKeyConstants.QUALITY, variantContext.getPhredScaledQual());
      row.set(Constants.ColumnKeyConstants.FILTER, variantToBqUtils.getFilters(variantContext));

      // Write calls to BQ row.
      List<TableRow> callRows = variantToBqUtils.addCalls(variantContext, vcfHeader);
      row.set(Constants.ColumnKeyConstants.CALLS, callRows);
    } catch (Exception e) {
      throw new RuntimeException("Invalid VCF file record: record line begin with: " +
              "Reference name:" + variantContext.getContig() + ", start position: " + variantContext.getStart() +
              ", end position: " + variantContext.getEnd() + ". " + e.getMessage());
    }
    return row;
  }
}
