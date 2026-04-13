#![allow(non_snake_case)]

use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jbyteArray, jint};
use std::mem;

// Konstanta Command ADB (Little Endian)
const A_CNXN: u32 = 0x4e584e43;
const A_AUTH: u32 = 0x48545541;
const A_OPEN: u32 = 0x4e45504f;

// Struct 24-byte persis. #[repr(C, packed)] krusial agar tidak ada padding memori.
#[repr(C, packed)]
struct AdbMessageHeader {
    command: u32,
    arg0: u32,
    arg1: u32,
    data_length: u32,
    data_check: u32,
    magic: u32,
}

// Fungsi helper pembuat header (Little Endian dipaksakan)
fn create_adb_header(command: u32, arg0: u32, arg1: u32, payload: &[u8]) -> AdbMessageHeader {
    let data_length = payload.len() as u32;
    // ADB checksum adalah penjumlahan byte sederhana (Unsigned Byte Sum)
    let data_check: u32 = payload.iter().map(|&b| b as u32).sum();
    let magic = command ^ 0xFFFFFFFF;

    AdbMessageHeader {
        command: command.to_le(),
        arg0: arg0.to_le(),
        arg1: arg1.to_le(),
        data_length: data_length.to_le(),
        data_check: data_check.to_le(),
        magic: magic.to_le(),
    }
}

// Konversi Struct ke Byte Slice
fn struct_to_bytes(header: &AdbMessageHeader) -> &[u8] {
    unsafe {
        std::slice::from_raw_parts(
            (header as *const AdbMessageHeader) as *const u8,
            mem::size_of::<AdbMessageHeader>(),
        )
    }
}

// ==================== JNI EXPORTS ====================

#[no_mangle]
pub extern "system" fn Java_com_proyek_adbengine_data_source_native_NativeAdbBridge_buildConnectMessage(
    env: JNIEnv, _: JClass, max_payload: jint,
) -> jbyteArray {
    let header = create_adb_header(A_CNXN, 0x01000000, max_payload as u32, &[]);
    let bytes = struct_to_bytes(&header);
    env.byte_array_from_slice(bytes).unwrap()
}

#[no_mangle]
pub extern "system" fn Java_com_proyek_adbengine_data_source_native_NativeAdbBridge_buildAuthSignatureMessage(
    env: JNIEnv, _: JClass, signature: jbyteArray,
) -> jbyteArray {
    let sig_bytes = env.convert_byte_array(signature).unwrap();
    let header = create_adb_header(A_AUTH, 2, 0, &sig_bytes);

    let mut full_message = Vec::with_capacity(24 + sig_bytes.len());
    full_message.extend_from_slice(struct_to_bytes(&header));
    full_message.extend_from_slice(&sig_bytes);

    env.byte_array_from_slice(&full_message).unwrap()
}

#[no_mangle]
pub extern "system" fn Java_com_proyek_adbengine_data_source_native_NativeAdbBridge_buildOpenMessage(
    mut env: JNIEnv, _: JClass, local_id: jint, dest: JString,
) -> jbyteArray {
    let dest_str: String = env.get_string(&dest).unwrap().into();
    // Tambahkan \0 (null terminator) yang diwajibkan ADB untuk payload string
    let mut payload = dest_str.into_bytes();
    payload.push(0);

    let header = create_adb_header(A_OPEN, local_id as u32, 0, &payload);

    let mut full_message = Vec::with_capacity(24 + payload.len());
    full_message.extend_from_slice(struct_to_bytes(&header));
    full_message.extend_from_slice(&payload);

    env.byte_array_from_slice(&full_message).unwrap()
}