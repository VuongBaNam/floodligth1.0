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

public interface NeighborsMethodInterface {
  public void scaleNeighbors(int width, int height, WeightElementVO[][] matrix,
                             WeightElementVO weight, double t, DistanceMethodInterface distanceMethod)
    throws SOMException;
  public void updateWeigh(SOMNode sample, WeightElementVO[][] matrix,
                          WeightElementVO weight, double t)
          throws SOMException;
  public void setRadius();
}
