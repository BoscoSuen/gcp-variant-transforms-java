// Copyright 2020 Google LLC

package com.google.gcp_variant_transforms.entity;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.GenotypesContext;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFHeader;
import org.junit.Before;
import org.junit.Test;
import java.util.Collections;

/**
 * Unit tests for Variant.java
 */
public class VariantTest {

  VariantContext variantContext;

  @Before
  public void mockVariantContext() {
    variantContext = mock(VariantContext.class);
    Allele allele = mock(Allele.class);
    GenotypesContext genotypesContext = mock(GenotypesContext.class);

    // The following fields cannot be null in VariantContext
    when(variantContext.getContig()).thenReturn("20");
    when(variantContext.getStart()).thenReturn(1);
    when(variantContext.getEnd()).thenReturn(2);
    when(variantContext.getSource()).thenReturn(".");
    when(variantContext.getID()).thenReturn(".");
    when(variantContext.getAlleles()).thenReturn(Collections.singletonList(allele));
    when(variantContext.getGenotypes()).thenReturn(genotypesContext);
  }

  @Test
  public void testGetHeader_whenComparingElements_thenTrue() {
    VCFHeader header = mock(VCFHeader.class);

    Variant testVariant = new Variant(variantContext, header);

    assertThat(testVariant.getHeader()).isEqualTo(header);
  }
}
