package com.google.gcp_variant_transforms.library;

import com.google.api.services.bigquery.model.TableRow;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFHeader;
import htsjdk.variant.vcf.VCFHeaderLineType;
import java.util.List;
import java.util.Set;

public interface VariantToBqUtils {
  public String getReferenceBases(VariantContext variantContext);

  public String getNames(VariantContext variantContext);

  public List<TableRow> getAlternateBases(VariantContext variantContext);

  public Set<String> getFilters(VariantContext variantContext);

  public void addInfo(TableRow row, VariantContext variantContext, List<TableRow> altMetadata,
                      VCFHeader vcfHeader);

  public List<TableRow> addCalls(VariantContext variantContext, VCFHeader vcfHeader);

  public Object convertToDefinedType(Object value, VCFHeaderLineType type, int count);

  public Object convertSingleObjectToDefinedType(Object value, VCFHeaderLineType type);

  public void addGenotypes(TableRow row, List<Allele> alleles, VariantContext variantContext);

  public void addInfoAndPhaseSet(TableRow row, Genotype genotype, VCFHeader vcfHeader);

  public void splitAlternateAlleleInfoFields(String attrName, Object value,
                                             List<TableRow> altMetadata, VCFHeaderLineType type);
}
