// Copyright 2020 Google LLC

package com.google.gcp_variant_transforms.entity;

import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFHeader;
import java.io.Serializable;

/**
 * Internal representation of Variant Data.
 * Variant inherits from {@link VariantContext} which is serializable
 * And Variant also stores the header information of the VCF file.
 */
public class Variant extends VariantContext implements Serializable {
  private static final long serialVersionUID = 260660913763642347L;

  private VCFHeader vcfHeader;

  public Variant(VariantContext variantContext, VCFHeader vcfHeader) {
    super(variantContext);
    this.vcfHeader = vcfHeader;
  }

  public VCFHeader getHeader() {
    return this.vcfHeader;
  }
}
