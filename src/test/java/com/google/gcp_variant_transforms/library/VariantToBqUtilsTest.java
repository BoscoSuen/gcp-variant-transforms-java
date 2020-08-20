// Copyright 2020 Google LLC

package com.google.gcp_variant_transforms.library;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.services.bigquery.model.TableRow;
import com.google.gcp_variant_transforms.TestEnv;
import com.google.gcp_variant_transforms.common.Constants;
import com.google.guiceberry.junit4.GuiceBerryRule;
import com.google.inject.Inject;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.GenotypesContext;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFormatHeaderLine;
import htsjdk.variant.vcf.VCFHeader;
import htsjdk.variant.vcf.VCFHeaderLineCount;
import htsjdk.variant.vcf.VCFHeaderLineType;
import htsjdk.variant.vcf.VCFInfoHeaderLine;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Units tests for VariantToBqUtilsImpl.java
 */
public class VariantToBqUtilsTest {
  private static final String TEST_ID= "id";
  private static final String TEST_REFERENCE_BASES = "G";
  private static final String TEST_ALTERNATE_BASES = "A";
  private static final String TEST_CALLS_NAME = "sample";
  private static final double TEST_AF = 0.333;
  private static final int TEST_NS = 2;
  private static final int DEFAULT_GENOTYPE = -1;

  VariantContext variantContext;
  GenotypesContext genotypesContext;
  VCFHeader vcfHeader;
  Genotype sample;
  Allele firstGenotypeAllele;
  Allele secondGenotypeAllele;
  Map<String, Object> extendedAttributes = new HashMap<>();

  @Rule
  public final GuiceBerryRule guiceBerry = new GuiceBerryRule(TestEnv.class);

  @Inject
  public VariantToBqUtils variantToBqUtils;

  @Before
  public void constructMockClasses() {
    variantContext = mock(VariantContext.class);
    vcfHeader = mock(VCFHeader.class);
    genotypesContext = mock(GenotypesContext.class);
    sample = mock(Genotype.class);
    firstGenotypeAllele = mock(Allele.class);
    secondGenotypeAllele = mock(Allele.class);
  }

  /**
   * Mock VCF Header Lines:
   * ##INFO=<ID=NS,Number=1,Type=Integer,Description="Number of Samples With Data">
   * ##INFO=<ID=DP,Number=1,Type=Integer,Description="Total Depth">
   * ##INFO=<ID=AF,Number=.,Type=Float,Description="Allele Frequency">
   * ##FORMAT=<ID=HQ,Number=2,Type=Integer,Description="Haplotype Quality">
   */
  @Before
  public void mockVCFHeader() {
    VCFInfoHeaderLine NSMetadata = mock(VCFInfoHeaderLine.class);
    when(vcfHeader.getInfoHeaderLine("NS")).thenReturn(NSMetadata);
    when(NSMetadata.getType()).thenReturn(VCFHeaderLineType.Integer);
    when(NSMetadata.getCountType()).thenReturn(VCFHeaderLineCount.INTEGER);
    when(NSMetadata.getCount()).thenReturn(1);

    VCFInfoHeaderLine AFMetadata = mock(VCFInfoHeaderLine.class);
    when(vcfHeader.getInfoHeaderLine("AF")).thenReturn(AFMetadata);
    when(AFMetadata.getType()).thenReturn(VCFHeaderLineType.Float);
    when(AFMetadata.getCountType()).thenReturn(VCFHeaderLineCount.A);

    // Mock VCF header in Calls.
    VCFFormatHeaderLine DPMetadata = mock(VCFFormatHeaderLine.class);
    when(vcfHeader.getFormatHeaderLine("DP")).thenReturn(DPMetadata);
    when(DPMetadata.getType()).thenReturn(VCFHeaderLineType.Integer);
    when(DPMetadata.getCountType()).thenReturn(VCFHeaderLineCount.INTEGER);
    when(DPMetadata.getCount()).thenReturn(1);

    VCFFormatHeaderLine HQMetadata = mock(VCFFormatHeaderLine.class);
    when(vcfHeader.getFormatHeaderLine("HQ")).thenReturn(HQMetadata);
    when(HQMetadata.getType()).thenReturn(VCFHeaderLineType.Integer);
    when(HQMetadata.getCountType()).thenReturn(VCFHeaderLineCount.INTEGER);
    when(HQMetadata.getCount()).thenReturn(2);
  }

  /**
   * Mock Genotype record:
   * FORMAT           sample
   * GT:DP:PS:HQ    1|2:1:0:23,27
   * GT:DP:HQ         .:1:.,.
   */
  @Before
  public void mockGenotypeContext() {
    when(sample.getSampleName()).thenReturn(TEST_CALLS_NAME);
    when(firstGenotypeAllele.getDisplayString()).thenReturn("G");
    when(variantContext.getAlleleIndex(firstGenotypeAllele)).thenReturn(1);
    when(secondGenotypeAllele.getDisplayString()).thenReturn("A");
    when(variantContext.getAlleleIndex(secondGenotypeAllele)).thenReturn(2);
    when(sample.getAlleles()).thenReturn(Arrays.asList(firstGenotypeAllele, secondGenotypeAllele));

    // Mock info.
    when(sample.hasAD()).thenReturn(false);
    when(sample.hasDP()).thenReturn(true);
    when(sample.getDP()).thenReturn(1);
    when(sample.hasGQ()).thenReturn(false);
    when(sample.hasPL()).thenReturn(false);
    extendedAttributes.put("HQ", "23,27");
    extendedAttributes.put("PS", "0");
    when(sample.getExtendedAttributes()).thenReturn(extendedAttributes);

    // Add sample to genotypeContext.
    Iterator<Genotype> genotypeIterator = mock(Iterator.class);
    when(genotypeIterator.hasNext()).thenReturn(true, false);
    when(genotypeIterator.next()).thenReturn(sample);
    when(genotypesContext.iterator()).thenReturn(genotypeIterator);
    when(variantContext.getGenotypes()).thenReturn(genotypesContext);
  }


  @Test
  public void testConvertStringValueToRightValueType_whenComparingElement_thenTrue() {
    // Test list of values.
    int count = 2;
    String integerListStr = "23,27";
    assertThat(variantToBqUtils.convertToDefinedType(integerListStr,
        VCFHeaderLineType.Integer, count))
        .isEqualTo(Arrays.asList(23, 27));
    String floatListStr = "0.333,0.667";
    assertThat(variantToBqUtils.convertToDefinedType(floatListStr, VCFHeaderLineType.Float, count))
        .isEqualTo(Arrays.asList(0.333, 0.667));

    // Test single values.
    count = 1;
    // Test int values.
    String integerStr = "23";
    assertThat(variantToBqUtils.convertToDefinedType(integerStr,
        VCFHeaderLineType.Integer, count))
        .isEqualTo(23);

    // Test float values.
    String floatStr = "0.333";
    assertThat(variantToBqUtils.convertToDefinedType(floatStr, VCFHeaderLineType.Float, count))
        .isEqualTo(0.333);

    // If input value is integer but type is float, it should be able to cast to float value.
    floatStr = "1";
    assertThat(variantToBqUtils.convertToDefinedType(floatStr, VCFHeaderLineType.Float, count))
            .isEqualTo(1.0);

    // Test String values.
    String str = "T";
    assertThat(variantToBqUtils.convertToDefinedType(str, VCFHeaderLineType.String, count))
        .isEqualTo("T");

    // Test value count does not match the number in the VCFHeader which will raise an exception
    int invalidCount = 2;
    Exception countNotMatchException = assertThrows(IndexOutOfBoundsException.class, () ->
            variantToBqUtils.convertToDefinedType(str, VCFHeaderLineType.String, invalidCount));
    assertThat(countNotMatchException).hasMessageThat().contains("Value count does not match the " +
            "count defined by VCFHeader");

    // Test value type does not match the type in the VCFHeader which will raise an exception
    String invalidFloatStr = "1.5";
    int validCount = 1;
    Exception numberFormatException = assertThrows(NumberFormatException.class, () ->
            variantToBqUtils.convertToDefinedType(invalidFloatStr,
        VCFHeaderLineType.Integer, validCount));
    assertThat(numberFormatException).hasMessageThat().contains("For input string");
  }

  @Test
  public void testGetReferenceBase_whenComparingElement_thenTrue() {
    Allele refAllele = mock(Allele.class);
    when(refAllele.getDisplayString()).thenReturn(TEST_REFERENCE_BASES);   // mock reference value
    when(variantContext.getReference()).thenReturn(refAllele);

    assertThat(variantToBqUtils.getReferenceBases(variantContext)).isEqualTo(TEST_REFERENCE_BASES);
  }

  @Test
  public void testGetNames_whenComparingElement_thenTrue() {
    when(variantContext.getID()).thenReturn(TEST_ID);
    assertThat(variantToBqUtils.getNames(variantContext)).isEqualTo(TEST_ID);
    // Test empty fields.
    when(variantContext.getID()).thenReturn(Constants.MISSING_FIELD_VALUE);
    assertThat(variantToBqUtils.getNames(variantContext)).isNull();

  }

  @Test
  public void testGetAlternates_whenComparingElement_thenTrue() {
    Allele altAllele = mock(Allele.class);
    when(altAllele.getDisplayString()).thenReturn("T");   // mock alternate value
    List<Allele> alternateList = new ArrayList<>();
    // If test alt field is MISSING_FIELD_VALUE("."), alternateList should be empty and TableRow
    // should set null
    when(variantContext.getAlternateAlleles()).thenReturn(alternateList);
    List<TableRow> missingFieldTableRow = variantToBqUtils.getAlternateBases(variantContext);
    assertThat(missingFieldTableRow.get(0)
        .get(Constants.ColumnKeyConstants.ALTERNATE_BASES_ALT)).isNull();

    // Test alt field has value "T".
    alternateList.add(altAllele);
    List<TableRow> altFieldTableRow = variantToBqUtils.getAlternateBases(variantContext);
    assertThat(altFieldTableRow.get(0)
        .get(Constants.ColumnKeyConstants.ALTERNATE_BASES_ALT)).isEqualTo("T");
  }


  /**
   * The unit test for addInfo() method should cover the test of the private method
   * `splitAlternateAlleleInfoFields`, which put info fields with number = `A` into alt field
   * Test info record:
   * NS = 2, the NS field number = 1, type is Integer
   * AF = 0.333, the AF field number = A, type is Float
   */
  @Test
  public void testAddInfo_whenCheckingAltAndInfoFieldElements_thenTrue() {
    Map<String, Object> info = new HashMap<>();
    info.put("NS", "2");
    info.put("AF", "0.333");
    when(variantContext.getAttributes()).thenReturn(info);

    // Mock alt field.
    List<TableRow> altMetadata = new ArrayList<>();
    altMetadata.add(new TableRow());
    altMetadata.get(0).set(Constants.ColumnKeyConstants.ALTERNATE_BASES_ALT, TEST_ALTERNATE_BASES);

    TableRow row = new TableRow();
    variantToBqUtils.addInfo(row, variantContext, altMetadata, vcfHeader);

    assertThat(row.containsKey("NS")).isTrue();
    assertThat(row.get("NS")).isEqualTo(TEST_NS);

    // AF field should not be in the info row, should be moved to alt field.
    assertThat(!row.containsKey("AF")).isTrue();
    assertThat(altMetadata.get(0).containsKey("AF")).isTrue();
    assertThat(altMetadata.get(0).get("AF")).isEqualTo(TEST_AF);
  }

  /**
   * The test for addCalls() methods should cover the test of the private method
   * 'addInfoAndPhaseSet' and 'addGenotypes'.
   * If phaseSet(PS) is presented, it should be removed from info map and instead set as phase set
   * If genotypes in missing, genotypes should be default value -1
   * If the phaseSet(PS) is not presented, phase set should be default "*"
   * Test Genotype:
   * FORMAT           sample
   * GT:DP:PS:HQ    1|2:1:0:23,27
   * GT:DP:HQ         .:1:.,.
   * In the second record, genotypes will set [-1,-1] and phase set will set "*",
   * and HQ will be [null, null]
   */
  @Test
  public void testAddCallsWithFieldValue_whenCheckingGenotypeElements_thenTrue() {
    // Test table row fields.
    List<TableRow> calls = variantToBqUtils.addCalls(variantContext, vcfHeader);
    TableRow row = calls.get(0);
    assertThat(row.get(Constants.ColumnKeyConstants.CALLS_NAME)).isEqualTo("sample");
    assertThat(row.get(Constants.ColumnKeyConstants.CALLS_GENOTYPE)).isEqualTo(Arrays.asList(1, 2));
    assertThat(row.get(Constants.ColumnKeyConstants.CALLS_PHASESET)).isEqualTo("0");
    // PS should not be in the info map, should be set in phase set
    assertThat(!row.containsKey("PS")).isTrue();
    assertThat(row.get("HQ")).isEqualTo(Arrays.asList(23, 27));
  }

  @Test
  public void testAddCallsWithEmptyFields_whenCheckingGenotypeElements_thenTrue() {
    // Second record: contains empty fields.
    // Mock unknown genotypes.
    when(firstGenotypeAllele.getDisplayString()).thenReturn(Constants.MISSING_FIELD_VALUE);
    when(secondGenotypeAllele.getDisplayString()).thenReturn(Constants.MISSING_FIELD_VALUE);
    when(sample.getAlleles()).thenReturn(Arrays.asList(firstGenotypeAllele, secondGenotypeAllele));

    // Mock empty info fields.
    extendedAttributes.remove("PS");
    extendedAttributes.put("HQ",".,.");
    when(sample.getExtendedAttributes()).thenReturn(extendedAttributes);

    // Add sample to genotypeContext.
    when(genotypesContext.iterator().hasNext()).thenReturn(true, false);
    when(genotypesContext.iterator().next()).thenReturn(sample);

    // Test table row fields.
    List<TableRow> callsWithEmptyFields = variantToBqUtils.addCalls(variantContext, vcfHeader);
    TableRow rowWithEmptyFields = callsWithEmptyFields.get(0);
    assertThat(rowWithEmptyFields.get(Constants.ColumnKeyConstants.CALLS_NAME))
        .isEqualTo(TEST_CALLS_NAME);
    assertThat(rowWithEmptyFields.get(Constants.ColumnKeyConstants.CALLS_GENOTYPE))
        .isEqualTo(Arrays.asList(DEFAULT_GENOTYPE, DEFAULT_GENOTYPE));
    assertThat(rowWithEmptyFields.get(Constants.ColumnKeyConstants.CALLS_PHASESET))
        .isEqualTo(Constants.DEFAULT_PHASESET);
    assertThat(rowWithEmptyFields.get("HQ")).isEqualTo(Arrays.asList(null,null));
  }
}
