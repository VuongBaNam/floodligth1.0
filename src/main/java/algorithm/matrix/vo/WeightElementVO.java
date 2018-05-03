
package algorithm.matrix.vo;

import java.util.ArrayList;
import java.util.List;

public class WeightElementVO {

  private int x;
  private int y;
  private int distance;
  private SOMNode weight;
  private List<SOMNode> group;

  public WeightElementVO(int x, int y, SOMNode weight) {
    setNewWeight(x, y, weight);
    
    group = null;
  }

  public List<SOMNode> getGroup() {
    return group;
  }

  public void addOnGroup(SOMNode element) {
    if (group == null)
      group = new ArrayList<SOMNode>(100);
    
    group.add(element);
  }

  public boolean hasMembers() {
    if (group == null)
      return false;

    return ! group.isEmpty();  
  }

  public void setNewWeight(int x, int y, SOMNode weight) {
    this.x = x;
    this.y = y;
    this.weight = weight;
  }

  public WeightElementVO multiply(double multiplier) {
    int i;
    SOMNode sample;
        
    sample = new SOMNode();
    
    for (i = 0; i < weight.getNumberOfValues(); i++) {
      if (weight.getValue(i) == null)
        sample.addValue(null);
      else
        sample.addValue(weight.getValue(i) * multiplier);
    }
    
    return new WeightElementVO(x, y, sample);
  }

  public WeightElementVO add(WeightElementVO adder) {
    int i;
    SOMNode sample;
	    
    sample = new SOMNode();
	    
    for (i = 0; i < weight.getNumberOfValues(); i++) {
      if (weight.getValue(i) != null && adder.getWeight().getValue(i) != null)
        sample.addValue(weight.getValue(i) + adder.getWeight().getValue(i));
      else
        sample.addValue(null);
    }
    
    return new WeightElementVO(x, y, sample);
  }

  public SOMNode getWeight() {
    return weight;
  }

  public int getXPosition() {
    return x;
  }

  public int getYPosition() {
    return y;
  }

  public void setXPosition(int x) {
    this.x = x;
  }

  public void setYPosition(int y) {
    this.y = y;
  }

  public void setPercentageDistance(int distance) {
    this.distance = distance; 
  }

  public int getPercentageDistance() {
    return distance;
  }

  public void setWeight(SOMNode weight) {
    this.weight = weight;
  }

  public void setGroup(List<SOMNode> group) {
    this.group = group;
  }
}
