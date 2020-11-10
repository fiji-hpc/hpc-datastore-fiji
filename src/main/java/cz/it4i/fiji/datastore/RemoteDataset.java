/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore;

import java.util.UUID;

import net.imagej.Dataset;

import org.janelia.saalfeldlab.n5.DataBlock;

public interface RemoteDataset<T> extends Dataset {

	UUID getUIUD();

	DatasetVersion getVersion();

	short getRx();

	short getRy();

	short getRz();

	DataBlock<T> getBlock(int x, int y, int z);

}
