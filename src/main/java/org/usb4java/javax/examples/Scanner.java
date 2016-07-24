package org.usb4java.javax.examples;

import java.io.IOException;
import java.util.List;

import javax.usb.UsbClaimException;
import javax.usb.UsbConfiguration;
import javax.usb.UsbConst;
import javax.usb.UsbControlIrp;
import javax.usb.UsbDevice;
import javax.usb.UsbDeviceDescriptor;
import javax.usb.UsbException;
import javax.usb.UsbHostManager;
import javax.usb.UsbHub;
import javax.usb.UsbInterface;
import javax.usb.UsbInterfacePolicy;

public class Scanner {
	private static final short	VENDOR_ID	= 0x0c2e;
	private static final short	PRODUCT_ID	= 0x0200;

	public static void main(String[] args) throws SecurityException, UsbException {
		UsbHub rootHub = UsbHostManager.getUsbServices().getRootUsbHub();
		UsbDevice scanner = findScanner(rootHub);

		if (scanner == null) {
			System.err.println("Scanner not found.");
			System.exit(1);
			return;
		}

		claimInterface(scanner);
		byte[] data = receiveMessage(scanner);
		System.out.println(data);

		try {
			System.in.read();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static byte[] receiveMessage(UsbDevice scanner) {
		byte[] data = { 0 };
		try {
			UsbControlIrp irp = scanner.createUsbControlIrp(
					(byte) (UsbConst.REQUESTTYPE_TYPE_CLASS | UsbConst.REQUESTTYPE_RECIPIENT_INTERFACE),
					UsbConst.REQUEST_GET_STATUS,
					(short) 0,
					(short) 0);

			data = irp.getData();
			scanner.syncSubmit(irp);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return data;
	}

	private static void claimInterface(UsbDevice scanner) throws UsbClaimException, UsbException {
		UsbConfiguration configuration = scanner.getUsbConfiguration((byte) 1);
		UsbInterface iface = configuration.getUsbInterface((byte) 0);

		List<UsbInterface> ifaces = configuration.getUsbInterfaces();

		for (UsbInterface intf : ifaces) {
			System.out.println(intf);
		}

		System.out.println("Interface: " + iface);

		iface.claim(new UsbInterfacePolicy() {
			@Override
			public boolean forceClaim(UsbInterface usbInterface) {
				return true;
			}
		});
	}

	@SuppressWarnings("unchecked")
	private static UsbDevice findScanner(UsbHub rootHub) {
		UsbDevice scanner = null;

		for (UsbDevice device : (List<UsbDevice>) rootHub.getAttachedUsbDevices()) {
			if (device.isUsbHub()) {
				scanner = findScanner((UsbHub) device);

				if (scanner != null) return scanner;
			} else {
				UsbDeviceDescriptor descriptor = device.getUsbDeviceDescriptor();

				if (descriptor.idVendor() == VENDOR_ID && descriptor.idProduct() == PRODUCT_ID) return device;
			}
		}

		return null;
	}
}
