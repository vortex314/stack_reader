package be.limero;

import java.io.File;
import java.io.IOException;

import nl.lxtreme.binutils.elf.Elf;
import nl.lxtreme.binutils.elf.Symbol;

public class Main {
	Elf elf;

	static boolean isInSymbol(Symbol symbol, long address) {
		if ((address >= symbol.getValue())
				&& (address < (symbol.getValue() + symbol.getSize())))
			return true;
		return false;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Reading ELF file " + args[0]);

		try {
			File file = new File(args[0]);
			Elf elf = new Elf(file);
			elf.loadSymbols();
			Symbol[] symbols = elf.getSymbols();
			for (int i = 0; i < symbols.length; i++) {
				System.out.println(symbols[i].toString() + " : "
						+ Long.toHexString(symbols[i].getValue()) + "-"
						+ symbols[i].getSize());
			}

		} catch (IOException e) {

			// System.out.printf("Hello world on epoch : %d" ,
			// System.currentTimeMillis());
			e.printStackTrace();
		}
	}

}
