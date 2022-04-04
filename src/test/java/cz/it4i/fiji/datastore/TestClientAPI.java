package cz.it4i.fiji.datastore;

import cz.it4i.fiji.legacy.ReadFullImage;
import cz.it4i.fiji.legacy.WriteFullImage;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imglib2.type.numeric.RealType;

import java.io.IOException;

public class TestClientAPI {
	public static void main(String[] args) {
		final ImageJ myIJ = new ImageJ();
		myIJ.ui().showUI();

		try {
			System.out.println("START READING");
			final ImgPlus<? extends RealType<?>> ip = ReadFullImage.from(
					"localhost:9080",
					"a0f2b1cc-9487-457e-9393-8448782f4221",
					0, 0, 0,
					4, 4, 2,
					"latest").getImgPlus();
			System.out.println("DONE READING");
			myIJ.ui().show(ip);
			System.out.println("DONE SHOWING");

			ip.getImg().forEach(p -> p.setReal( p.getRealFloat() + 10.f ));

			System.out.println("START WRITING");
			WriteFullImage.to(ip,
					"localhost:9080",
					"a0f2b1cc-9487-457e-9393-8448782f4221",
					0, 0, 0,
					4, 4, 2,
					"new");
			System.out.println("DONE WRITING");
		} catch (IOException e) {
			System.out.println("ERROR getting the image: "+e.getMessage());
			//e.printStackTrace();
		}
	}
}
