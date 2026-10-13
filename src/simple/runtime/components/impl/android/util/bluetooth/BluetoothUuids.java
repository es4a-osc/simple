package simple.runtime.components.impl.android.util.bluetooth;

import java.util.UUID;

/**
 * 蓝牙 RFCOMM 服务标识；每个组件保存自己的选择，避免组件之间互相覆盖 UUID。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class BluetoothUuids {

	public static final UUID CHAT = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
	public static final UUID SERIAL = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

	private BluetoothUuids() {
	}
}
