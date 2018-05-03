package algorithm.distance;


import algorithm.exception.SOMException;
import algorithm.matrix.vo.SOMNode;

public interface DistanceMethodInterface {

  public double calculateDistance(SOMNode e1, SOMNode e2) throws SOMException;
}
