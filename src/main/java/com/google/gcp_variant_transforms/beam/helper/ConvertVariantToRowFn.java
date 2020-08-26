// Copyright 2020 Google LLC

package com.google.gcp_variant_transforms.beam.helper;

import com.google.api.services.bigquery.model.TableRow;
import com.google.gcp_variant_transforms.library.BigQueryRowGenerator;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFHeader;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.TupleTag;

/** {@link DoFn} implementation for a VariantContext to a BigQuery Row. */
public class ConvertVariantToRowFn extends DoFn<VariantContext, TableRow> {
  private boolean allowMalformedRecords = true;
  private final BigQueryRowGenerator bigQueryRowGenerator;
  private final VCFHeader vcfHeader;

  public ConvertVariantToRowFn(BigQueryRowGenerator bigQueryRowGenerator, VCFHeader vcfHeader,
                               boolean allowMalformedRecords) {
    this.bigQueryRowGenerator = bigQueryRowGenerator;
    this.vcfHeader = vcfHeader;
    this.allowMalformedRecords = allowMalformedRecords;
  }

  public static final TupleTag<TableRow> VALID_VARIANT_TO_BQ_RECORD_TAG = new TupleTag<>() {
  };

  public static final TupleTag<TableRow> INVALID_VARIANT_TO_BQ_RECORD_TAG = new TupleTag<>() {
  };


  @ProcessElement
  public void processElement(@Element VariantContext variantContext, MultiOutputReceiver receiver) {
    try {
      receiver.get(VALID_VARIANT_TO_BQ_RECORD_TAG)
          .output(bigQueryRowGenerator.convertToBQRow(variantContext, vcfHeader));
    } catch (Exception e) {
      if (allowMalformedRecords) {
        receiver.get(INVALID_VARIANT_TO_BQ_RECORD_TAG)
            .output(bigQueryRowGenerator.convertToBQRow(variantContext, vcfHeader));
      } else {
        throw new RuntimeException(e.getMessage());
      }
    }
  }
}
