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

import algorithm.exception.SOMException;
import algorithm.matrix.vo.SOMNode;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;

public class SampleVectorFile implements SampleVectorInterface {

  private List<String> header;
  private List<SOMNode> vector;
  private Random randomize;
  private String name;

  public SampleVectorFile(String file) throws SOMException {
    if (file == null)
      throw new SOMException("File is not valid");
    
    this.name = file;
    vector    = new ArrayList<SOMNode>(100);
    header    = new ArrayList<String>(100);
    randomize = new Random();
    
    openFile(file);
  }

  protected final void openFile(String fileName) 
    throws SOMException {
    File file;	    
    String line;
    
    InputStream is;
    BufferedReader reader;
    
    file = new File(fileName);
    
    if (!file.exists())
      throw new SOMException("File does not exists");

    try {
      is = new FileInputStream(file);
    } 
    catch (FileNotFoundException e) {
      throw new SOMException("File not found", e);    
    }
    
    reader = new BufferedReader(new InputStreamReader(is));
    
    try {
      parseHeader(reader.readLine());

      while ((line = reader.readLine()) != null)
        vector.add(parseLine(line));
    } 
    catch (IOException e) {
      throw new SOMException("Problems with reader", e);
    }
  }

  private final String normalizeLine(String line) {	  
    while (line.contains("\t\t"))
      line = line.replaceAll("\t\t", "\t \t");

    return line;
  }

  protected final void parseHeader(String line) throws SOMException {
    StringTokenizer tokenizer;
	    
    line = normalizeLine(line);
    
    tokenizer = new StringTokenizer(line, ",");

    while (tokenizer.hasMoreTokens())
      header.add(tokenizer.nextToken());	    
  }

  protected final SOMNode parseLine(String line) throws SOMException {
    String token;
    Double value;
    SOMNode element;
    StringTokenizer tokenizer;

    tokenizer = new StringTokenizer(line, ",");

    if (!tokenizer.hasMoreTokens())
      throw new SOMException("Problems with the Sample file");
    
    element = new SOMNode();

    if (!tokenizer.hasMoreTokens())
      throw new SOMException("Problems with the Sample file");

    for(int i = 0;i < 5;i++){
      if ((token = tokenizer.nextToken()).equals(" "))
        value = null;
      else
        value = new Double(token);

      element.addValue(value);
    }
    element.setCategory(new Double(tokenizer.nextToken()));
    
    return element;
  }

  @Override
  public List<SOMNode> getVector() {
    return vector;
  }

  public List<String> getHeader() {
    return header;
  }

  public String getName() {
    return name;
  }
  

  public SOMNode getElement(final int idx) {
    return vector.get(idx);
  }

  public SOMNode randomizeWeight() {	  
	  int i;
	  int row;
    SOMNode weight;
    Double value;
    
    value = null;
    
    weight = new SOMNode();
    
    for (i = 0; i < getColSize(); i++) {
      while (value == null) {
        row = randomize.nextInt(getRowSize());
        value = getElement(row).getValue(i);
      }

      weight.addValue(value);
    }
    
    return weight;
  }

  public SOMNode randomizeSample() {
    int row;
    
    row = randomize.nextInt(getRowSize());
    
    return getElement(row);
  }

  public int getRowSize() {
    return vector.size();
  }

  public int getColSize() {
    return (header.size() - 1);
  }
}
