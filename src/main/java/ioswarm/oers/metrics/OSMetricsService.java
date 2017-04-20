package ioswarm.oers.metrics;

import java.util.Date;

import org.hyperic.sigar.Cpu;
import org.hyperic.sigar.CpuInfo;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.NetInfo;
import org.hyperic.sigar.NetInterfaceConfig;
import org.hyperic.sigar.OperatingSystem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.Swap;
import org.hyperic.sigar.Uptime;

import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import ioswarm.oers.OERSService;
import ioswarm.oers.util.DateUtil;
import ioswarm.oers.util.Util;

public class OSMetricsService extends OERSService {

	private final Sigar sigar = new Sigar();;
	
	@Override
	public void start() {
		
		EventBus eb = vertx.eventBus();
		
		eb.consumer("oers.metrics.os", this::handleOSMetrixEvent);
	}
	
	public JsonObject osInfo() {
		OperatingSystem os = OperatingSystem.getInstance();
		
		JsonObject v = new JsonObject()
				.put("description", os.getDescription())
				.put("name", os.getName())
				.put("version", os.getVersion())
				.put("arch", os.getArch())
				.put("patch_level", os.getPatchLevel())
				.put("machine", os.getMachine())
				.put("cpu_endian", os.getCpuEndian())
				.put("data_model", os.getDataModel())
				.put("vendor", os.getVendor())
				.put("vendor_name", os.getVendorName())
				.put("vendor_version", os.getVendorVersion())
				.put("vendor_codename", os.getVendorCodeName());
		
		return v;
	}
	
	public JsonObject interfactInfo(NetInterfaceConfig cfg) throws SigarException {
		return new JsonObject()
				.put("interface", cfg.getName())
				.put("ip_address", cfg.getAddress())
				.put("mac_address", cfg.getHwaddr())
				.put("netmask", cfg.getNetmask());
	}
	
	public JsonObject networkInfo() throws SigarException {
		NetInfo ifo = sigar.getNetInfo();
		
		JsonObject net = new JsonObject()
				.put("primary", interfactInfo(sigar.getNetInterfaceConfig()))
				.put("hostname", ifo.getHostName())
				.put("domainname", ifo.getDomainName())
				.put("default_gateway", ifo.getDefaultGateway())
				.put("primary_dns", ifo.getPrimaryDns())
				.put("secondary_dns", ifo.getSecondaryDns());

		JsonArray ifaces = new JsonArray();
		for (String iface : sigar.getNetInterfaceList())
			ifaces.add(interfactInfo(sigar.getNetInterfaceConfig(iface)));
		net.put("interfaces", ifaces);
		
		return net;
	}
	
	public JsonObject cpuInfo() throws SigarException {
		Cpu c = sigar.getCpu();
		
		
		final JsonObject cpu = new JsonObject()
				.put("total", c.getTotal())
				.put("sys", c.getSys())
				.put("user", c.getUser())
				.put("idle", c.getIdle())
				.put("wait", c.getWait())
				.put("stolen", c.getStolen())
				.put("irq", c.getIrq())
				.put("soft_irq", c.getSoftIrq());
		
		int i = 0;
		JsonArray socks = new JsonArray();
		for (CpuInfo ifo : sigar.getCpuInfoList()) {
			socks.add(new JsonObject()
					.put("socket", i)
					.put("vendor", ifo.getVendor())
					.put("model", ifo.getModel())
					.put("mhz", ifo.getMhz())
					.put("cache_size", ifo.getCacheSize())
					.put("cores_per_socket", ifo.getCoresPerSocket())
					.put("total_cores", ifo.getTotalCores())
					.put("total_sockets", ifo.getTotalSockets())
			);
			i++;
		}
		cpu.put("sockets", socks);
		
		return cpu;
	}
	
	public JsonObject memInfo() throws SigarException {
		Mem mem = sigar.getMem();
		Swap swap = sigar.getSwap();
		
		JsonObject memory = new JsonObject()
				.put("ram", new JsonObject()
						.put("total", mem.getTotal())
						.put("used", mem.getUsed())
						.put("free", mem.getFree())
						.put("actual_used", mem.getActualUsed())
						.put("actual_free", mem.getActualFree())
						.put("used_percent", mem.getUsedPercent())
						.put("free_percent", mem.getFreePercent())
				)
				.put("swap", new JsonObject()
						.put("total", swap.getTotal())
						.put("used", swap.getUsed())
						.put("free", swap.getFree())
						.put("page_in", swap.getPageIn())
						.put("page_out", swap.getPageOut())
				);
		
		return memory;
	}
	
	public void handleOSMetrixEvent(Message<JsonObject> msg) {
		vertx.executeBlocking((Future<JsonObject> f) -> {
			try {
				Uptime up = sigar.getUptime();
				
				JsonObject os = new JsonObject()
						.put("currentTime", DateUtil.toString("yyyy-MM-dd HH:mm:ss.s", new Date()))
						.put("uptime", up.getUptime())
						.put("os", osInfo())
						.put("network", networkInfo())
						.put("memory", memInfo())
						.put("cpu", cpuInfo());
				
				f.complete(os);
			} catch(Exception e) {
				f.fail(e);
			}
		}, hdl -> {
			if (hdl.succeeded()) msg.reply(hdl.result());
			else msg.fail(500, Util.getStackTrace(hdl.cause()));
		});
	}
	
}
