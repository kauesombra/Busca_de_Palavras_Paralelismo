import org.jocl.*;
import static org.jocl.CL.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;


public class ParallelGPU {

    private static final String KERNEL =
        "__kernel void count(                                    \n" +
        "  __global const int*  starts,                         \n" +
        "  __global const int*  lens,                           \n" +
        "  __global const char* text,                           \n" +
        "  __global const char* target,                         \n" +
        "  int tlen,                                            \n" +
        "  __global int* out)                                   \n" +
        "{                                                       \n" +
        "  int i = get_global_id(0);                            \n" +
        "  if (lens[i] != tlen) { out[i] = 0; return; }        \n" +
        "  for (int j = 0; j < tlen; j++)                      \n" +
        "    if (text[starts[i]+j] != target[j]) {             \n" +
        "      out[i] = 0; return;                              \n" +
        "    }                                                   \n" +
        "  out[i] = 1;                                          \n" +
        "}                                                       \n";

    public static long[] run(String filePath, String word) throws Exception {
        String raw    = new String(Files.readAllBytes(Paths.get(filePath))).toLowerCase();
        byte[] tbytes = raw.getBytes("US-ASCII");
        byte[] wbytes = word.toLowerCase().getBytes("US-ASCII");
        int    wlen   = wbytes.length;

        // Tokenizar: registra início e tamanho de cada token no array de bytes
        List<Integer> sList = new ArrayList<>();
        List<Integer> lList = new ArrayList<>();
        int i = 0;
        while (i < tbytes.length) {
            while (i < tbytes.length && !isAlNum(tbytes[i])) i++;
            if (i >= tbytes.length) break;
            int s = i;
            while (i < tbytes.length && isAlNum(tbytes[i])) i++;
            sList.add(s);
            lList.add(i - s);
        }

        int n        = sList.size();
        int[] starts = new int[n];
        int[] lens   = new int[n];
        int[] out    = new int[n];
        for (int k = 0; k < n; k++) {
            starts[k] = sList.get(k);
            lens[k]   = lList.get(k);
        }

        long inicio = System.currentTimeMillis();

        // ── Setup OpenCL ─────────────────────────────────────────────────────
        CL.setExceptionsEnabled(true);

        cl_platform_id[] plats = new cl_platform_id[1];
        clGetPlatformIDs(1, plats, null);

        cl_device_id[] devs = new cl_device_id[1];
        clGetDeviceIDs(plats[0], CL_DEVICE_TYPE_ALL, 1, devs, null);

        cl_context ctx       = clCreateContext(null, 1, devs, null, null, null);
        cl_command_queue q   = clCreateCommandQueueWithProperties(ctx, devs[0], null, null);

        // ── Buffers ──────────────────────────────────────────────────────────
        cl_mem mStarts = clCreateBuffer(ctx, CL_MEM_READ_ONLY  | CL_MEM_COPY_HOST_PTR, (long) Sizeof.cl_int * n,  Pointer.to(starts), null);
        cl_mem mLens   = clCreateBuffer(ctx, CL_MEM_READ_ONLY  | CL_MEM_COPY_HOST_PTR, (long) Sizeof.cl_int * n,  Pointer.to(lens),   null);
        cl_mem mText   = clCreateBuffer(ctx, CL_MEM_READ_ONLY  | CL_MEM_COPY_HOST_PTR, (long) tbytes.length,      Pointer.to(tbytes), null);
        cl_mem mWord   = clCreateBuffer(ctx, CL_MEM_READ_ONLY  | CL_MEM_COPY_HOST_PTR, (long) wlen,               Pointer.to(wbytes), null);
        cl_mem mOut    = clCreateBuffer(ctx, CL_MEM_WRITE_ONLY,                         (long) Sizeof.cl_int * n,  null,               null);

        // ── Kernel ───────────────────────────────────────────────────────────
        cl_program prog = clCreateProgramWithSource(ctx, 1, new String[]{ KERNEL }, null, null);
        clBuildProgram(prog, 0, null, null, null, null);
        cl_kernel ker   = clCreateKernel(prog, "count", null);

        clSetKernelArg(ker, 0, Sizeof.cl_mem, Pointer.to(mStarts));
        clSetKernelArg(ker, 1, Sizeof.cl_mem, Pointer.to(mLens));
        clSetKernelArg(ker, 2, Sizeof.cl_mem, Pointer.to(mText));
        clSetKernelArg(ker, 3, Sizeof.cl_mem, Pointer.to(mWord));
        clSetKernelArg(ker, 4, Sizeof.cl_int, Pointer.to(new int[]{ wlen }));
        clSetKernelArg(ker, 5, Sizeof.cl_mem, Pointer.to(mOut));

        clEnqueueNDRangeKernel(q, ker, 1, null, new long[]{ n }, null, 0, null, null);
        clEnqueueReadBuffer(q, mOut, CL_TRUE, 0, (long) Sizeof.cl_int * n, Pointer.to(out), 0, null, null);

        // ── Soma resultados ──────────────────────────────────────────────────
        int count = 0;
        for (int r : out) count += r;
        long tempo = System.currentTimeMillis() - inicio;

        // ── Libera recursos OpenCL ───────────────────────────────────────────
        clReleaseMemObject(mStarts); clReleaseMemObject(mLens);
        clReleaseMemObject(mText);   clReleaseMemObject(mWord); clReleaseMemObject(mOut);
        clReleaseKernel(ker);        clReleaseProgram(prog);
        clReleaseCommandQueue(q);    clReleaseContext(ctx);

        return new long[]{ count, tempo };
    }

    static boolean isAlNum(byte b) {
        return (b >= 'a' && b <= 'z') || (b >= '0' && b <= '9');
    }
}
