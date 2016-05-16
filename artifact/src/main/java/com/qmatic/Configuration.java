package com.qmatic;

import java.util.Properties;

import org.restexpress.RestExpress;
import org.restexpress.util.Environment;

public class Configuration
extends Environment
{
	private static final String DEFAULT_EXECUTOR_THREAD_POOL_SIZE = "20";

	private static final String PORT_PROPERTY = "port";
	private static final String BASE_URL_PROPERTY = "base.url";
	private static final String EXECUTOR_THREAD_POOL_SIZE = "executor.threadPool.size";
	private static final String BACKLOG_QUEUE_HIGH_WATER_MARK = "regulator.hwm";
	private static final String BACKLOG_QUEUE_LOW_WATER_MARK = "regulator.lwm";
	private static final String BACKLOG_QUEUE_AIMED_MARK = "regulator.am";
	private static final String REGULATOR_ALGORITHM = "regulator.algorithm";
    private static final String REGULATOR_TCR_SCALING_FACTOR = "regulator.TCRScalingFactor";
	private static final String REGULATOR_INITIAL_TCR = "regulator.initialTCR";
	private static final String REGULATOR_TCR_LIVE_UPDATE = "regulator.TCRLiveUpdate";
	private static final String REGULATOR_CC = "regulator.CC";
	private static final String REGULATOR_FAIRNESS = "regulator.fairness";

	private int port;
	private String baseUrl;
	private int executorThreadPoolSize;
    private int highWaterMark;
    private int lowWaterMark;
    private int aimedMark;
	private String algorithm;
    private double TCRScalingFactor;
	private double initialTCR;
	private boolean TCRLiveUpdate;
    private double CC;
	private boolean fairness;

	private SampleController sampleController;
    private RegulatorDebugController regulatorDebugController;

	@Override
	protected void fillValues(Properties p)
	{
		this.port = Integer.parseInt(p.getProperty(PORT_PROPERTY, String.valueOf(RestExpress.DEFAULT_PORT)));
		this.baseUrl = p.getProperty(BASE_URL_PROPERTY, "http://localhost:" + String.valueOf(port));
		this.executorThreadPoolSize = Integer.parseInt(p.getProperty(EXECUTOR_THREAD_POOL_SIZE, DEFAULT_EXECUTOR_THREAD_POOL_SIZE));
        setUpRegulator(p);
		initialize();
	}

    private void setUpRegulator(Properties p) {
        this.highWaterMark = Integer.parseInt(p.getProperty(BACKLOG_QUEUE_HIGH_WATER_MARK));
        this.lowWaterMark = Integer.parseInt(p.getProperty(BACKLOG_QUEUE_LOW_WATER_MARK));
        this.aimedMark = Integer.parseInt(p.getProperty(BACKLOG_QUEUE_AIMED_MARK));
        this.algorithm = p.getProperty(REGULATOR_ALGORITHM);
        this.TCRScalingFactor = Double.parseDouble(p.getProperty(REGULATOR_TCR_SCALING_FACTOR));
		this.initialTCR = Double.parseDouble(p.getProperty(REGULATOR_INITIAL_TCR));
		this.TCRLiveUpdate = Boolean.parseBoolean(p.getProperty(REGULATOR_TCR_LIVE_UPDATE));
        this.CC = Double.parseDouble(p.getProperty(REGULATOR_CC));
		this.fairness = Boolean.parseBoolean(REGULATOR_FAIRNESS);
    }

    private void initialize()
	{
		sampleController = new SampleController(this);
        regulatorDebugController = new RegulatorDebugController(sampleController);
	}

	public int getPort()
	{
		return port;
	}

	public String getBaseUrl()
	{
		return baseUrl;
	}

	public int getExecutorThreadPoolSize()
	{
		return executorThreadPoolSize;
	}

	public SampleController getSampleController()
	{
		return sampleController;
	}

    public RegulatorDebugController getRegulatorDebugController() {
        return regulatorDebugController;
    }

    public int getLowWaterMark() {
        return lowWaterMark;
    }

    public int getHighWaterMark() {
        return highWaterMark;
    }

    public int getAimedMark() {
        return aimedMark;
    }

    public String getRegulatorAlgorithm(){
        return algorithm;
    }

    public double getTCRScalingFactor() {
        return TCRScalingFactor;
    }

	public double getInitialTCR() {
		return initialTCR;
	}

	public boolean isTCRLiveUpdate() {
		return TCRLiveUpdate;
	}

    public double getCC() {
        return CC;
    }

	public boolean isFairness() {
		return fairness;
	}
}
