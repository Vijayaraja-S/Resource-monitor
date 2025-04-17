package com.p3.resourcemonitor;

import io.micrometer.core.instrument.MeterRegistry;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import org.springframework.stereotype.Component;

@Component
public class SystemMetricsCollector {

    private long[] previousTicks;

    public SystemMetricsCollector(MeterRegistry registry) {
        SystemInfo si = new SystemInfo();
        CentralProcessor cpu = si.getHardware().getProcessor();
        GlobalMemory memory = si.getHardware().getMemory();
        HWDiskStore[] diskStores = si.getHardware().getDiskStores().toArray(new HWDiskStore[0]);

        // Initialize previousTicks with the current CPU ticks
        previousTicks = cpu.getSystemCpuLoadTicks();

        // Registering CPU metrics with correct CPU load calculation
        registry.gauge("system.cpu.load", cpu, processor -> {
            long[] currentTicks = processor.getSystemCpuLoadTicks();
            double load = processor.getSystemCpuLoadBetweenTicks(previousTicks);
            previousTicks = currentTicks;  // Update previous ticks
            return load;
        });
        
        registry.gauge("system.cpu.count", cpu, CentralProcessor::getLogicalProcessorCount);

        // Registering Memory metrics
        registry.gauge("system.memory.total", memory, GlobalMemory::getTotal);
        registry.gauge("system.memory.available", memory, GlobalMemory::getAvailable);

        // Registering Disk usage metrics
        for (HWDiskStore diskStore : diskStores) {
            registry.gauge("system.disk.usage.total", diskStore, store -> store.getSize());
            registry.gauge("system.disk.usage.free", diskStore, store -> store.getSize());
            registry.gauge("system.disk.usage.readBytes", diskStore, store -> store.getReads());
            registry.gauge("system.disk.usage.writeBytes", diskStore, store -> store.getWrites());
        }
    }
}
