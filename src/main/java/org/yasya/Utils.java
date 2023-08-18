package org.yasya;

import java.awt.Color;
import java.util.Random;

public class Utils {
	public static Random random = new Random();
	
	public static int[] smash(int sum) {
		int n1 = -1;
		for(int i = (sum + 2); i > 2; i--) {
			if(random.nextInt(i) <= 1) {
				n1 = sum + 2 - i;
				break;
			};
		}
		int n2 = random.nextInt(sum - n1 + 1);
		int n3 = sum - n1 - n2;
		return new int[] {n1, n2, n3};
	}

	public static Color[] getPalette(int size) {
		Color[] colors = new Color[size];
		for(int i = 0; i < colors.length; i++) {
			int index = i % Constants.TETRIS_PALETTE.length;
			colors[i] = new Color(Constants.TETRIS_PALETTE[index][0], Constants.TETRIS_PALETTE[index][1], Constants.TETRIS_PALETTE[index][2]);
		}
		return colors;
	}

	public static double fire(Chainable chain, double initialT, int STEP_COUNT) {
		chain.afterStart(chain);
		double score = chain.score();
		double t = initialT;
		double bestScore = 9999;
		Chainable bestChain = null;
		for(int i = 0; i < STEP_COUNT; i++) {
			Chainable newChain = chain.next();
			double newScore = newChain.score();
			if(newScore <= score) {
				score = newScore;
				chain = newChain;
				if(bestScore > score) {
					bestScore = score;
					bestChain = chain;
				}
				chain.afterBetterSolutionFound(chain, bestScore, t);
			}
			else {
				double p = Math.exp(-(double)(newScore - score) / t);
				if(random.nextDouble() < p) {
					score = newScore;
					chain = newChain;
					chain.afterBetterSolutionFound(chain, bestScore, t);
				}
			}
		
			t = initialT * ((double)(STEP_COUNT - i)) / ((double)STEP_COUNT);
			
			if(i % 100 == 0) {
				if(chain.checkStop()) break;
			}
			if(i % (STEP_COUNT / 100) == 0) {
				chain.onProgress(i * 100 / STEP_COUNT);
			}
		}
		chain.beforeFinish(chain, bestChain);
		return bestScore;
	}

	public static int[] randomPermutation(int length) {
		int[] permutation = new int[length];
		for (int i = 0; i < length; i++) {
			permutation[i] = i;
		}
		for (int i = length - 1; i > 0; i--) {
			int j = random.nextInt(i + 1);
			int temp = permutation[i];
			permutation[i] = permutation[j];
			permutation[j] = temp;
		}

		return permutation;
	}

	public static double distance(double x1, double y1, double x2, double y2) {
		return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
	} 
}
