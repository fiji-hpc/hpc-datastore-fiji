/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2022 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.legacy;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, headless = true,
	menuPath = "File>Import>From HPC-Datastore...")
public class FileImport extends ReadFullImage {


}
