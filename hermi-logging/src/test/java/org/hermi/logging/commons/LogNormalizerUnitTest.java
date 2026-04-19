package org.hermi.logging.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hermi.logging.commons.util.Customer;
import org.hermi.logging.commons.util.MaskCustomer;
import org.hermi.logging.commons.util.SSNCustomer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

public class LogNormalizerUnitTest {
  private LogNormalizer logNormalizer;

  @BeforeEach
  public void setup() {
    logNormalizer = LogNormalizer.getInstance();
  }

  @Test
  public void test_normalize_array_primitive() {
    Object[] args = new Object[] {"test", 123, true};
    String result = logNormalizer.normalize(args, true).toString();

    ObjectMapper mapper = new ObjectMapper();

    assertEquals(mapper.readTree("[\"test\", 123, true]"), mapper.readTree(result));
  }

  @Test
  public void test_normalize_array_object() {
    Object[] args = new Object[] {new Customer("123456789"), new Customer("234567891", "First")};
    String result = logNormalizer.normalize(args, false).toString();

    ObjectMapper mapper = new ObjectMapper();

    assertEquals(
        mapper.readTree(
            "[{\"ssn\":\"123456789\",\"firstName\":null},{\"ssn\":\"234567891\",\"firstName\":\"First\"}]"),
        mapper.readTree(result));
  }

  @Test
  public void test_normalize_array_mix() {
    Object[] args = new Object[] {"test", new Customer("234567891", "First")};
    String result = logNormalizer.normalize(args, false).toString();

    ObjectMapper mapper = new ObjectMapper();

    assertEquals(
        mapper.readTree("[\"test\",{\"ssn\":\"234567891\",\"firstName\":\"First\"}]"),
        mapper.readTree(result));
  }

  @Test
  public void test_resolve_object_required_only() {
    Customer customer = new Customer("123456789");
    String result = logNormalizer.normalize(customer).toString();
    ObjectMapper mapper = new ObjectMapper();

    assertEquals(mapper.readTree("{\"ssn\":\"123456789\"}"), mapper.readTree(result));
  }

  @Test
  public void test_resolve_object_all() {
    Customer customer = new Customer("123456789", "first");
    String result = logNormalizer.normalize(customer, false).toString();
    ObjectMapper mapper = new ObjectMapper();

    assertEquals(
        mapper.readTree("{\"ssn\":\"123456789\",\"firstName\":\"first\"}"),
        mapper.readTree(result));
  }

  @Test
  public void test_normalize_mask_greater_than_4() {
    MaskCustomer customer = new MaskCustomer("234567891");
    String result = logNormalizer.normalize(customer, false).toString();

    ObjectMapper mapper = new ObjectMapper();

    assertEquals(mapper.readTree("{\"ssn\":\"*****7891\"}"), mapper.readTree(result));
  }

  @Test
  public void test_normalize_mask_not_greater_than_4() {
    MaskCustomer customer = new MaskCustomer("7891");
    String result = logNormalizer.normalize(customer, false).toString();

    ObjectMapper mapper = new ObjectMapper();

    assertEquals(mapper.readTree("{\"ssn\":\"**91\"}"), mapper.readTree(result));
  }

  @Test
  public void test_normalize_ssn() {
    SSNCustomer customer = new SSNCustomer("234567891");
    String result = logNormalizer.normalize(customer, false).toString();

    ObjectMapper mapper = new ObjectMapper();

    assertEquals(mapper.readTree("{\"ssn\":\"***-**-7891\"}"), mapper.readTree(result));
  }

  @Test
  public void test_normalize_ssn_wrong() {
    SSNCustomer customer = new SSNCustomer("67891");
    String result = logNormalizer.normalize(customer, false).toString();

    ObjectMapper mapper = new ObjectMapper();

    assertEquals(mapper.readTree("{\"ssn\":\"***-**-7891\"}"), mapper.readTree(result));
  }
}
