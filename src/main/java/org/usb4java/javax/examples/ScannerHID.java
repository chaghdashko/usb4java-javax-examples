package org.usb4java.javax.examples;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import org.usb4java.BufferUtils;
import org.usb4java.DeviceHandle;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

public class ScannerHID {
	private static final short	VENDOR_ID		= 0x0c2e;		// Metrologic Instruments
	private static final short	PRODUCT_ID		= 0x0200;		// MS7120 Barcode Scanner
	private static final int	INTERFACE		= 0;			// Interface number
	private static final byte	ENDPOINT_IN		= (byte) 0x81;	// Interrupt input endpoint
	private static final int	TRANSFER_SIZE	= 1;			// Barcode data size in bytes

	public static void main(String[] args) {
		DeviceHandle handle = null;

		try {
			init();
			handle = open(VENDOR_ID, PRODUCT_ID);
			claimInterface(handle, INTERFACE);
			transferData(handle, INTERFACE, ENDPOINT_IN, 0, TRANSFER_SIZE);
		} catch (LibUsbException e) {
			e.printStackTrace();
		} finally {
			releaseInterface(handle, INTERFACE);
			close(handle);

			System.out.println("Exiting default context");
			LibUsb.exit(null);
		}
	}

	private static void init() throws LibUsbException {
		int r = LibUsb.init(null);

		if (r != LibUsb.SUCCESS) {
			throw new LibUsbException("ERROR: Context cannot be initialized", r);
		} else {
			System.out.println("Context initialized");
		}
	}

	private static DeviceHandle open(short vendorId, short productId) throws LibUsbException {
		DeviceHandle handle = LibUsb.openDeviceWithVidPid(null, vendorId, productId);

		if (handle == null) {
			throw new LibUsbException("ERROR: Device handle null", LibUsb.ERROR_OTHER);
		} else {
			System.out.println("USB device opened");
		}

		return handle;
	}

	private static void claimInterface(DeviceHandle handle, int intfNum) throws LibUsbException {
		int r = LibUsb.claimInterface(handle, intfNum);

		if (r != LibUsb.SUCCESS) {
			throw new LibUsbException("ERROR: Unable to claim interface", r);
		} else {
			System.out.println("Interface claimed");
		}
	}

	private static String transferData(DeviceHandle handle, int intfNum, byte endpoint, int timeout, int size)
			throws LibUsbException {
		byte[] arr = new byte[TRANSFER_SIZE];

		for (int i = 0; i < TRANSFER_SIZE; i++) {
			arr[i] = 0;
		}

		ByteBuffer data = BufferUtils.allocateByteBuffer(TRANSFER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
		IntBuffer transferred = BufferUtils.allocateIntBuffer();
		int r;
		List<Byte> bytes = new ArrayList<>();
		boolean isFirst = true;
		boolean isSecond = true;

		while (true) {
			data.rewind();

			if (isFirst || isSecond) {
				r = LibUsb.interruptTransfer(handle, endpoint, data, transferred, 0);
			} else {
				r = LibUsb.interruptTransfer(handle, endpoint, data, transferred, 100);
			}

			if (r != LibUsb.SUCCESS) {
				if (r != LibUsb.ERROR_TIMEOUT) {
					throw new LibUsbException("ERROR: Unable read data", r);
				} else break;
			} else {
				int readCount = transferred.get();
				data.get(arr, 0, readCount);

				if (!isFirst) bytes.add(arr[0]);

				if (isFirst == true) isFirst = false;
				else if (isSecond == true) isSecond = false;

				transferred.clear();
				data.clear();
			}
		}

		System.out.println("Read count = " + bytes.size());

		for (Byte b : bytes) {
			System.out.print(b + " ");
		}

		System.out.println();
		System.out.println();

		return data.toString();
	}

	private static void releaseInterface(DeviceHandle handle, int intfNum) {
		if (handle != null) {
			int r = LibUsb.releaseInterface(handle, intfNum);

			if (r != LibUsb.SUCCESS) {
				System.err.println("ERROR: Unable to release interface! Result code: " + r);
			} else {
				System.out.println("Interface released");
			}
		}
	}

	private static void close(DeviceHandle handle) {
		if (handle != null) {
			LibUsb.close(handle);
		}
	}
}
