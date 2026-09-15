package com.nasrally.nasrally.views

import androidx.compose.runtime.Composable

@JsFun("""
(source, callback) => {
    var input = document.createElement('input');
    input.type = 'file';
    input.accept = 'image/*';
    if (source === 'CAMERA') {
        input.setAttribute('capture', 'environment');
    }
    input.onchange = function(e) {
        var file = e.target.files[0];
        if (!file) return;
        var reader = new FileReader();
        reader.onload = function(event) {
            var img = new Image();
            img.onload = function() {
                var canvas = document.createElement('canvas');
                canvas.width = img.width;
                canvas.height = img.height;
                var ctx = canvas.getContext('2d');
                ctx.drawImage(img, 0, 0);
                var pngUrl = canvas.toDataURL('image/png');
                var base64 = pngUrl.split(',')[1];
                var binary = atob(base64);
                var len = binary.length;
                var bytes = new Int8Array(len);
                for (var i = 0; i < len; i++) {
                    bytes[i] = binary.charCodeAt(i);
                }
                var str = Array.from(bytes).join(',');
                callback(str);
            };
            img.src = event.target.result;
        };
        reader.readAsDataURL(file);
    };
    input.click();
}
""")
private external fun jsLaunchImagePicker(source: String, callback: (JsAny) -> Unit)

@Composable
actual fun rememberImagePicker(
    source: ImageSource,
    onImagePicked: (ByteArray) -> Unit
): () -> Unit {
    return {
        try {
            jsLaunchImagePicker(source.name) { jsStr ->
                val str = jsStr.toString()
                if (str.isNotEmpty()) {
                    val byteList = str.split(',').mapNotNull { it.trim().toByteOrNull() }
                    onImagePicked(byteList.toByteArray())
                }
            }
        } catch (e: Exception) {
            println("Wasm image picker error: ${e.message}")
        }
    }
}
