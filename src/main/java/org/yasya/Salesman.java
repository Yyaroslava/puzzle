package org.yasya;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.swing.SwingWorker;

public class Salesman extends SwingWorker<Void, Integer> {
	public double[][] towns;
	public double bestScore = 9999998;
	public Solution bestSolution = null;
	public Solution secondSolution = null;
	public double secondScore = 9999999;
	private long startTime = System.currentTimeMillis();
	public double[] history = new double[Constants.SALESMAN_HISTORY_COUNT];
	public int historyIndex = 0;

	public Salesman() {
		towns = new double[Constants.SALESMAN_TOWNS_COUNT][2];
		for(int i = 0; i < Constants.SALESMAN_TOWNS_COUNT; i++) {
			towns[i][0] = Utils.random.nextDouble() * 300;
			towns[i][1] = Utils.random.nextDouble() * 300;
		}
	}

	synchronized public void addHistory(boolean jumped, double newScore) {
		if(jumped){
			history[historyIndex] = newScore;
		}
		else{
			history[historyIndex] = 0;
		}
		historyIndex = (historyIndex + 1) % history.length;
		if(historyIndex == 0) {
			Utils.updateChart(history);
		}
	}
	
	public class Solution implements Chainable, Runnable {
		public int[] path;
		public double score;
		public ThreadLocalRandom localRandom;

		public Solution(int[] path, double score) {
			this.path = path;
			this.score = score;
			this.localRandom = ThreadLocalRandom.current();
		}

		public Solution() {
			this.path = Utils.randomPermutation(Constants.SALESMAN_TOWNS_COUNT);
			this.calculateScore();
		}

		@Override
		public void afterBetterSolutionFound(Chainable s, double score, double t) {
			setBest((Solution)s, score, t);
		}

		@Override
		public void afterJump(boolean jumped, double newScore) {
			addHistory(jumped, newScore);
		}

		public void calculateScore() {
			int start = path[0];
			int end = path[Constants.SALESMAN_TOWNS_COUNT - 1];
			double score = Utils.distance(towns[start][0], towns[start][1], towns[end][0], towns[end][1]);
			for(int i = 0; i < Constants.SALESMAN_TOWNS_COUNT - 1; i++) {
				start = path[i];
				end = path[(i + 1) % Constants.SALESMAN_TOWNS_COUNT];
				score += Utils.distance(towns[start][0], towns[start][1], towns[end][0], towns[end][1]);
			}
			this.score = score;
		}

		@Override
		public Chainable next() {
			Solution s = copy();
			int i1 = localRandom.nextInt(Constants.SALESMAN_TOWNS_COUNT);
			int i2 = (i1 + localRandom.nextInt(Constants.SALESMAN_TOWNS_COUNT - 2)) % Constants.SALESMAN_TOWNS_COUNT;
			s.reflect(i1, i2);
			s.calculateScore();

			return s;
		}

		public void reflect(int start, int finish) {
			int left = 0;
			int right = (finish - start + Constants.SALESMAN_TOWNS_COUNT) % Constants.SALESMAN_TOWNS_COUNT;
			for (;left < right; left++, right--) {
				int i1 = (left + start) % Constants.SALESMAN_TOWNS_COUNT;
				int i2 = (right + start) % Constants.SALESMAN_TOWNS_COUNT;
				int t = path[i1];
				path[i1] = path[i2];
				path[i2] = t;
			}
		}

		@Override
		public double score() {
			return score;
		}

		public Solution copy() {
			int[] pathCopy = Arrays.copyOf(path, path.length);
			Solution s = new Solution(pathCopy, this.score);
			return s;
		}

		public void run() {
			try {
				System.out.println("Salesman fire");
				Utils.fire(this);
			} 
			catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public synchronized void onProgress(int progress) {
			publish(progress);
		}

		public String toString() {
			return Arrays.toString(path);
		}

	}
	
	public synchronized void setBest(Solution newSolution, double newScore, double t) {
		if(newScore < bestScore) {
			secondScore = bestScore;
			secondSolution = bestSolution;
			bestScore = newScore;
			bestSolution = newSolution.copy();
			System.out.printf("better solution found: %8.1f %8.5f \n", bestScore, t);
			BufferedImage image;
			if(secondSolution == null){
				image = SalesmanPNG.getAreaImage(340, 400, towns, bestSolution.path, null);
			}
			else{
				image = SalesmanPNG.getAreaImage(340, 400, towns, bestSolution.path, secondSolution.path);
			}
			UI.areaIcon.setImage(image);
			UI.scoreLabel.setText(String.format("better solution found: %6.1f", bestScore));
			UI.areaLabel.repaint();
		}
		else if(newScore == bestScore) {}
		else if(newScore < secondScore) {
			secondScore = newScore;
			secondSolution = newSolution.copy();
			System.out.printf("better second solution found: %8.1f %8.5f \n", secondScore, t);
			BufferedImage image = SalesmanPNG.getAreaImage(340, 400, towns, bestSolution.path, secondSolution.path);
			UI.areaIcon.setImage(image);
			UI.areaLabel.repaint();
		}
	}

	@Override
	protected Void doInBackground() throws Exception {
		publish(0);
		UI.currentTemperature = Constants.SALESMAN_INITIAL_T;
		UI.temperatureLabel.setText("t = " + Double.toString(Constants.SALESMAN_INITIAL_T));
		Solution initialSolution = new Solution();
		Thread[] sh = Stream.generate(() -> new Thread(initialSolution.copy()))
			.limit(Constants.SALESMAN_PARALLEL)
			.toArray(Thread[]::new);

		Arrays.stream(sh).forEach(Thread::start);
		Arrays.stream(sh).forEach(t -> {
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}); 
		
		publish(100);

		return null;
	}

	@Override
	protected void process(java.util.List<Integer> chunks) {
		int latestProgress = chunks.get(chunks.size() - 1);
		UI.progressBar.setValue(latestProgress);
	}

	@Override
	protected void done() {
		long duration = System.currentTimeMillis() - startTime;
		System.out.printf("best score: %f, duration: %d ms", bestScore, duration);
		try (BufferedWriter writer = new BufferedWriter(new FileWriter("Salesman.txt"))) {
			writer.write(bestSolution.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		BufferedImage image = SalesmanPNG.getAreaImage(800, 800, towns, bestSolution.path, secondSolution.path);
		File outputFile = new File("Salesman.png");
		try {
			ImageIO.write(image, "png", outputFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

