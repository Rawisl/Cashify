package com.example.cashify.utils;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;

// Decorator pattern để lắng nghe tiến trình (progress) của RequestBody gốc
public class CountingRequestBody extends RequestBody {

    public interface ProgressListener {
        void onProgress(long bytesWritten, long contentLength);
    }

    private final RequestBody delegate;
    private final ProgressListener listener;

    public CountingRequestBody(RequestBody delegate, ProgressListener listener) {
        this.delegate = delegate;
        this.listener = listener;
    }

    @Override
    public MediaType contentType() {
        return delegate.contentType();
    }

    @Override
    public long contentLength() throws IOException {
        return delegate.contentLength();
    }

    @Override
    public void writeTo(@NonNull BufferedSink sink) throws IOException {
        // Bọc luồng ghi (sink) để đếm số byte đã đẩy lên server
        BufferedSink countingSink = Okio.buffer(new ForwardingSink(sink) {
            long bytesWritten = 0;

            @Override
            public void write(@NonNull Buffer source, long byteCount) throws IOException {
                super.write(source, byteCount);
                bytesWritten += byteCount;
                listener.onProgress(bytesWritten, contentLength());
            }
        });

        delegate.writeTo(countingSink);
        countingSink.flush();
    }
}