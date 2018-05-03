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
package algorithm.matrix;

import algorithm.distance.DistanceMethodInterface;
import algorithm.exception.SOMException;
import algorithm.matrix.vo.SOMNode;
import algorithm.matrix.vo.WeightElementVO;
import algorithm.neighbors.NeighborsMethodInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WeightMatrix {
  private int width;
  private int height;
  private SampleVectorInterface sampleVector;
  private NeighborsMethodInterface neighborsMethod;
  private DistanceMethodInterface distanceMethod;
  private WeightElementVO[][] matrix;
  private Random randomize;

  public WeightMatrix(int width, int height, SampleVectorInterface sampleVector,
                      NeighborsMethodInterface neighborsMethod, DistanceMethodInterface distanceMethod)
          throws SOMException {
    int i, j;

    if (neighborsMethod == null)
      throw new SOMException("Not valid neighbors method");

    this.width  = width;
    this.height = height;

    randomize = new Random();

    this.sampleVector    = sampleVector;
    this.neighborsMethod = neighborsMethod;
    this.distanceMethod  = distanceMethod;

    matrix = new WeightElementVO[width][height];

    for (i = 0; i < width; i++)
      for (j = 0; j < height; j++) {
        matrix[i][j] = new WeightElementVO(i, j, sampleVector.randomizeWeight());
      }
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public List<WeightElementVO> getKBestWeight(WeightElementVO[][] matrix,SOMNode sample) throws SOMException {
    int i, j;
    double bestDistance, currDistance;

    List<WeightElementVO> matchList = new ArrayList<WeightElementVO>();;
    WeightElementVO first = null;
    WeightElementVO second = null;
    WeightElementVO threeth = null;

    bestDistance = Double.MAX_VALUE;

    for (i = 0; i < width; i++) {
      for (j = 0; j < height; j++) {
        currDistance = distanceMethod.calculateDistance(sample,
                matrix[i][j].getWeight());

        if (currDistance <= bestDistance) {
          bestDistance = currDistance;
          threeth = matrix[i][j];
          second = threeth;
          first = second;
        }
      }
    }
    matchList.add(first);
    matchList.add(second);
    matchList.add(threeth);
    return matchList;
  }

  public WeightElementVO getBestMatchingWeight(WeightElementVO[][] matrix,
                                               SOMNode sample) throws SOMException {
    int i, j;
    double bestDistance, currDistance;
    double category = sample.getCategory();

    List<WeightElementVO> matchList = new ArrayList<WeightElementVO>();

    bestDistance = Double.MAX_VALUE;

    matchList = null;

    for (i = 0; i < width; i++) {
      for (j = 0; j < height; j++) {
//        System.out.println(sample.getElements());
//        System.out.println(matrix[i][j].getWeight().getElements());
        currDistance = distanceMethod.calculateDistance(sample,
                matrix[i][j].getWeight());

        if (currDistance == bestDistance)
          matchList.add(matrix[i][j]);
        else if (currDistance < bestDistance) {
          bestDistance = currDistance;
          matchList = new ArrayList<WeightElementVO>();
          matchList.add(matrix[i][j]);
        }
      }
    }
    WeightElementVO weightElementVO = matchList.get(randomize.nextInt(matchList.size()));
    weightElementVO.getWeight().setCategory(category);
    return weightElementVO;
  }

  protected WeightElementVO getBestUMatchingWeight(WeightElementVO[][] matrix,
                                                   SOMNode sample) throws SOMException {
    int i, j;
    double bestDistance, currDistance;

    List<WeightElementVO> matchList;

    bestDistance = Double.MAX_VALUE;

    matchList = null;

    for (i = 0; i < (width*2); i += 2) {
      for (j = 0; j < (height*2); j += 2) {
        currDistance = distanceMethod.calculateDistance(sample,
                matrix[i][j].getWeight());

        if (currDistance == bestDistance)
          matchList.add(matrix[i][j]);
        else if (currDistance < bestDistance) {
          bestDistance = currDistance;
          matchList = new ArrayList<WeightElementVO>();
          matchList.add(matrix[i][j]);
        }
      }
    }

    return matchList.get(randomize.nextInt(matchList.size()));
  }

  public void executeStepLearn(double t) throws SOMException {
    //SOMNode sample;
    WeightElementVO bestWeight;//neurons win

//    sample = sampleVector.randomizeSample();
//    bestWeight = getBestMatchingWeight(matrix, sample);

    for(SOMNode sample : sampleVector.getVector()){
      bestWeight = getBestMatchingWeight(matrix, sample);
//      neighborsMethod.scaleNeighbors(width, height, matrix,
//              bestWeight, t, distanceMethod);
      neighborsMethod.updateWeigh(sample,matrix,bestWeight,t);
    }
    neighborsMethod.setRadius();
  }

  protected void setUMatrixDistances(WeightElementVO[][] groups) {
    int i, j;
    for (i = 0; i < groups.length; i++) {
      for (j = 0; j < groups[i].length; j++) {
        if (j%2 == 0)
          groups[i][j].setPercentageDistance(0);
        else
          groups[i][j].setPercentageDistance(100);
      }
    }
  }

  public WeightElementVO[][] mountUMatrix(SampleVectorInterface sampleVector)
          throws SOMException {
    System.out.println(sampleVector);
    int i, j, tmpI, tmpJ;
    SOMNode element;
    WeightElementVO group;
    WeightElementVO[][] groups;

    groups = new WeightElementVO[width*2][height*2];

    for (i = 0, tmpI = 0; tmpI < (width*2); i++, tmpI += 2) {
      for (j = 0, tmpJ = 0 ; tmpJ < (height*2); j++, tmpJ += 2) {
        groups[tmpI][tmpJ] = new WeightElementVO(tmpI, tmpJ, matrix[i][j].getWeight());
      }
    }

    for (i = 1; i < (width*2); i +=2 )
      for (j = 0; j < (height*2); j += 2)
        groups[i][j] = new WeightElementVO(i, j, null);

    for (i = 0; i < (width*2); i++) {
      for (j = 1; j < (height*2); j += 2)
        groups[i][j] = new WeightElementVO(i, j, null);
    }

    setUMatrixDistances(groups);

    for (i = 0; i < sampleVector.getRowSize(); i++) {
      element = sampleVector.getElement(i);
      group = getBestUMatchingWeight(groups, element);
      group.addOnGroup(element);
    }

    return groups;
  }

  public WeightElementVO[][] mountGroups(SampleVectorInterface sampleVector) throws SOMException {
    int i, j;
    SOMNode element;
    WeightElementVO group;
    WeightElementVO[][] groups;

    groups = new WeightElementVO[width][height];

    for (i = 0; i < width; i++)
      for (j = 0; j < height; j++)
        groups[i][j] = new WeightElementVO(i, j, matrix[i][j].getWeight());

    for (i = 0; i < sampleVector.getRowSize(); i++) {
      element = sampleVector.getElement(i);
      group = getBestMatchingWeight(groups, element);
      group.addOnGroup(element);
    }

    return groups;
  }

  public void setWidth(int width) {
    this.width = width;
  }

  public void setHeight(int height) {
    this.height = height;
  }

  public SampleVectorInterface getSampleVector() {
    return sampleVector;
  }

  public void setSampleVector(SampleVectorInterface sampleVector) {
    this.sampleVector = sampleVector;
  }

  public NeighborsMethodInterface getNeighborsMethod() {
    return neighborsMethod;
  }

  public void setNeighborsMethod(NeighborsMethodInterface neighborsMethod) {
    this.neighborsMethod = neighborsMethod;
  }

  public DistanceMethodInterface getDistanceMethod() {
    return distanceMethod;
  }

  public void setDistanceMethod(DistanceMethodInterface distanceMethod) {
    this.distanceMethod = distanceMethod;
  }

  public WeightElementVO[][] getMatrix() {
    return matrix;
  }

  public void setMatrix(WeightElementVO[][] matrix) {
    this.matrix = matrix;
  }
}