/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2023 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.bdv;

import java.io.IOException;

import org.janelia.saalfeldlab.n5.DataBlock;

interface DatasetServer<T> {

	DataBlock<T> readBlock(long[] coords, int time, ViewSetupValues setupValues)
		throws IOException;


}
