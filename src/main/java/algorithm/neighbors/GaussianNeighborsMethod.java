/* Copyright (C) 2006 Leonardo Bispo de Oliveira and 
 *                    Daniele Sunaga de Oliveira
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package algorithm.neighbors;

import algorithm.distance.DistanceMethodInterface;
import algorithm.exception.SOMException;
import algorithm.matrix.vo.SOMNode;
import algorithm.matrix.vo.WeightElementVO;

import java.util.ArrayList;
import java.util.List;

public class GaussianNeighborsMethod implements NeighborsMethodInterface {

  private final static double RADIUS = 60;
  private double radius;
  public GaussianNeighborsMethod() {
    this(RADIUS);
  }

  public GaussianNeighborsMethod(double radius) {
    this.radius = radius;
  }

  public void scaleNeighbors(int width, int height, WeightElementVO[][] matrix,
                             WeightElementVO weight, double t, DistanceMethodInterface distanceMethod)
          throws SOMException {
    int i, j;
    int x, y;

    double r, nullDecimal;
    SOMNode outer;
    SOMNode center;

    WeightElementVO newWeight;

    double t1, distanceNormalize, distance;

    r = Math.round((double) (radius * (1.0f - t))/2.0d);

    nullDecimal = 0.0d;

    outer  = new SOMNode();
    center = new SOMNode();

    outer.addMultiValue(r, r, nullDecimal);

    center.addMultiValue(nullDecimal, nullDecimal, nullDecimal);

    distanceNormalize = distanceMethod.calculateDistance(center, outer);

    for (i = (int) -r; i < r; i++) {
      for (j = (int) -r; j < r; j++) {
        if ((i + weight.getXPosition()) >= 0 &&
                (i + weight.getXPosition()) < width &&
                (j + weight.getYPosition()) >= 0 &&
                (j + weight.getYPosition()) < height) {
          outer  = new SOMNode();
          outer.addMultiValue(new Double(i), new Double(j), nullDecimal);
          distance = distanceMethod.calculateDistance(outer, center);
          distance /= distanceNormalize;

          t1 = Math.exp(-1.0d * (Math.pow(distance, 2.0d)) / 0.15d);
          t1 /= (t * 4.0d + 1.0d);

          x = weight.getXPosition() + i;
          y = weight.getYPosition() + j;

          newWeight = weight.multiply(t1).add(matrix[x][y].multiply((1.0f - t)));

          newWeight.setXPosition(x);
          newWeight.setYPosition(y);
          newWeight.getWeight().setCategory(weight.getWeight().getCategory());
          matrix[x][y] = newWeight;
        }
      }
    }
  }

  @Override
  public void updateWeigh(SOMNode sample,WeightElementVO[][] matrix, WeightElementVO bestWeigh,double t) throws SOMException {
    WeightElementVO newWeight ;
    int x = bestWeigh.getXPosition();
    int y = bestWeigh.getYPosition();
    for (int i = 0;i < matrix.length;i++)
      for (int j = 0;j < matrix.length;j++){
        SOMNode somNode = new SOMNode();
        List<Double> list = new ArrayList<>();
        for(int k = 0;k < bestWeigh.getWeight().getNumberOfValues();k++){
          double old = matrix[i][j].getWeight().getValue(k);
          double newW = old + radius*(sample.getValue(k) - old)/100;
          list.add(newW);
        }
        somNode.setElements(list);
        newWeight = new WeightElementVO(i,j,somNode);
        matrix[i][j] = newWeight;
      }
    if(t == 1){
      if(!isContains(bestWeigh.getGroup(),sample)){
        bestWeigh.addOnGroup(sample);
      }
    }
    matrix[x][y] = bestWeigh;
  }
  public void setRadius(){
    this.radius = this.radius*1.0/2;
  }

  public boolean isContains(List<SOMNode> list , SOMNode somNode){
    if(list == null){
      return false;
    }
    for(SOMNode node : list){
      if(node.getNumberOfValues() != somNode.getNumberOfValues()){
        return false;
      }
      for(int i = 0;i < node.getNumberOfValues();i++){
        if(node.getValue(i) != somNode.getValue(i)){
          return false;
        }
      }
    }
    return true;
  }
}
