/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package proofs;

import java.io.IOException;
import java.util.Map.Entry;

import org.janelia.saalfeldlab.n5.DataBlock;

import bdv.img.n5.N5ImageLoader;
import cz.it4i.fiji.datastore.DatasetServerImpl;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.sequence.ViewId;

public class ProofSpimData {

	public static void main(String[] args) throws SpimDataException, IOException {
		DatasetServerImpl dsi = new DatasetServerImpl(
			"/home/koz01/Desktop/fiji/work/rex-n5/export.xml");

		DataBlock<?> block = dsi.read(new long[] { 0, 0, 0 }, 0, 1, 0, new int []{ 1, 1, 1 });

	}
}
