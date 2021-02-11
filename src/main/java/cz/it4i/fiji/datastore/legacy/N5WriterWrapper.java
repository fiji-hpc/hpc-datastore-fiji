/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.legacy;

import org.janelia.saalfeldlab.n5.N5Writer;

import lombok.AllArgsConstructor;
import lombok.experimental.Delegate;


@AllArgsConstructor
public class N5WriterWrapper implements N5Writer {

	@Delegate
	private final N5Writer wrapped;


}
