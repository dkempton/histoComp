package edu.gsu.dmlab.tracking;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.FutureTask;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.common.util.concurrent.ListeningExecutorService;

import edu.gsu.dmlab.datatypes.interfaces.ITrack;
import edu.gsu.dmlab.tracking.interfaces.ITrackingFactory;

public class HistogramComparison implements FutureCallback<Boolean> {

	ListeningExecutorService executor;
	ITrackingFactory factory;
	String type;

	int maxThreads;
	int pageSize;
	Lock lock;
	Condition condNotFull;
	Condition condDoneProcessing;
	boolean doneProcessing = false;

	LinkedList<FutureTask<Boolean>> calcAndSaveList;

	public HistogramComparison(ITrackingFactory factory,
			ListeningExecutorService executor, String type, int maxThreads,
			int pageSize) {
		if (executor == null)
			throw new IllegalArgumentException(
					"ListeningExecutorService cannot be null in HistogramComparison constructor.");
		if (factory == null)
			throw new IllegalArgumentException(
					"ITrackingFactory cannot be null in HistogramComparison constructor.");

		this.type = type;
		this.factory = factory;
		this.executor = executor;
		this.maxThreads = maxThreads;
		this.pageSize = pageSize;

		this.calcAndSaveList = new LinkedList<FutureTask<Boolean>>();

		this.lock = new ReentrantLock();
		this.condNotFull = this.lock.newCondition();
		this.condDoneProcessing = this.lock.newCondition();
	}

	public void run(ITrack[] tracks, int offset) {

		int[][] paramCombos = this.factory
				.getParamCombos(offset, this.pageSize);
		while ((paramCombos != null) && (paramCombos.length != 0)) {
			for (int i = 0; i < paramCombos.length; i++) {
				System.out.println("Processing type {" + this.type
						+ "} param combo Id: " + paramCombos[i][0]);
				this.createProcessingTask(tracks, paramCombos[i]);
			}
			offset += this.pageSize;
			paramCombos = this.factory.getParamCombos(offset, this.pageSize);
		}

		this.lock.lock();
		this.doneProcessing = true;
		this.lock.unlock();

		this.lock.lock();
		try {
			if (this.calcAndSaveList.size() > 0) {
				this.condDoneProcessing.await();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			this.lock.unlock();
		}
	}

	private void createProcessingTask(ITrack[] tracks, int[] paramCombos) {

		this.lock.lock();
		try {
			while (this.calcAndSaveList.size() >= this.maxThreads) {
				this.condNotFull.await();
			}

			ListenableFutureTask<Boolean> processTask = (ListenableFutureTask<Boolean>) this.executor
					.submit(this.factory.getCallableCalcAndSaveHistDistances(
							this.type, tracks, paramCombos));
			Futures.addCallback(processTask, this, this.executor);
			this.calcAndSaveList.add(processTask);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			this.lock.unlock();
		}
	}

	private void handleClassificationTaskFinished() {
		this.lock.lock();
		try {
			Iterator<FutureTask<Boolean>> itr = this.calcAndSaveList.iterator();

			while (itr.hasNext()) {
				FutureTask<Boolean> tsk = itr.next();
				if (tsk.isDone()) {
					itr.remove();
					this.condNotFull.signal();
				}
			}
		} finally {
			this.lock.unlock();
		}

		try {
			this.lock.lock();
			if (this.doneProcessing && this.calcAndSaveList.size() == 0) {
				this.condDoneProcessing.signal();
			}

		} finally {
			this.lock.unlock();
		}
	}

	@Override
	public void onFailure(Throwable arg0) {
		System.out.println("Failed to calc Histogram and save.");
		arg0.printStackTrace();
		this.handleClassificationTaskFinished();
	}

	@Override
	public void onSuccess(Boolean arg0) {
		this.handleClassificationTaskFinished();
	}

}
