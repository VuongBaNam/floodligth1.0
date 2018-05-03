
package algorithm.matrix;

import algorithm.matrix.vo.SOMNode;

import java.util.List;

public interface SampleVectorInterface {

  public List<SOMNode> getVector();

  public List<String> getHeader();

  public String getName();

  public SOMNode getElement(final int idx);

  public SOMNode randomizeWeight();

  public SOMNode randomizeSample();

  public int getRowSize();

  public int getColSize(); 
}
