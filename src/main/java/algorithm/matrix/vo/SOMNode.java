
package algorithm.matrix.vo;

import java.util.ArrayList;
import java.util.List;

public final class SOMNode {

  private double category;
  private List<Double> elements;

  public SOMNode() {
    elements = new ArrayList<Double>();
  }

  public int getNumberOfValues() {
    return elements.size();
  }

  public Double getValue(final int idx) {
    return elements.get(idx);
  }

  public void addValue(final Double value) {
    this.elements.add(value);
  }

  public void addMultiValue(final Double...arguments) {
    for (Double argument : arguments)
      this.elements.add(argument);
  }

  public List<Double> getElements() {
    return elements;
  }

  public void setElements(List<Double> elements) {
    this.elements = elements;
  }

  public double getCategory() {
    return category;
  }

  public void setCategory(double category) {
    this.category = category;
  }
}
