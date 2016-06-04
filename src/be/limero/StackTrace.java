package be.limero;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;

import nl.lxtreme.binutils.elf.Attribute;
import nl.lxtreme.binutils.elf.Elf;
import nl.lxtreme.binutils.elf.ProgramHeader;
import nl.lxtreme.binutils.elf.Section;
import nl.lxtreme.binutils.elf.Symbol;

public class StackTrace implements Runnable {
	String _elfFileName;
	Elf _elf;
	Symbol[] _symbol;
	Section[] _sections;
	ProgramHeader[] _programHeaders;

	boolean isInSymbol(Symbol symbol, long address) {
		if ((address >= symbol.getValue()) && (address < (symbol.getValue() + symbol.getSize())))
			return true;
		return false;
	}

	String findSymbol(long address) {
		for (int i = 0; i < _symbol.length; i++) {
			if (isInSymbol(_symbol[i], address)) {
				return _symbol[i].getName();
			}
		}
		return "NOT_FOUND";
	}

	void listSymbols() {

	}

	void load(String fileName) {
		try {
			_elfFileName = fileName;
			File file = new File(fileName);
			_elf = new Elf(file);
			_elf.loadSymbols();
			_symbol = _elf.getSymbols();
			Attribute attributes = _elf.getAttributes();
			System.out.println(" CPU " + attributes.getCPU() + " little endian " + attributes.isLittleEndian()
					+ " type " + attributes.getType());
			_sections = _elf.getSections();
			for (int i = 0; i < _sections.length; i++) {
				System.out.println(" Section " + _sections[i].getName());
			}
			_programHeaders = _elf.getProgramHeaders();
			for (int i = 0; i < _programHeaders.length; i++) {
				System.out.println(" Header " + _programHeaders[i].getTypeName());
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	String[] symType = { "NOTYPE", "OBJECT", "FUNC" };

	void analyzeLine(String line) {
		if (line.contains("@(#):") || line.contains("epc1=0x")) {
			// System.out.print(line);
			if (line.contains("@(#):")) {
				int offset = line.indexOf("@(#):");
				String rest = line.substring(offset + 5, line.length());
				String[] part = rest.split(":");
				System.out.print(rest);
				if (part.length > 1) {
					long address = Long.parseLong(part[1], 16);
					Symbol symbol = _elf.getSymbol(address);
					String type;
					if (address < 0x40000000)
						type = "DATA";
					else
						type = "CODE";
					System.out.printf(" %4s %s +0x%X ", type, symbol.getName(), address - symbol.getValue());
				}
				System.out.println("");
			} else if (line.contains("epc1=0x")) {
				int offset = line.indexOf("epc1=0x");
				String rest = line.substring(offset + 7, line.length());
				String[] part = rest.split(",");
				System.out.println(rest);
				if (part.length > 1) {
					long address = Long.parseLong(part[0], 16);
					Symbol symbol = _elf.getSymbol(address);
					String type;
					if (address < 0x40000000)
						type = "DATA";
					else
						type = "CODE";
					System.out.printf(" %4s %s +0x%X\n", type, symbol.getName(), address - symbol.getValue());
				}

			}
		} /*else {
			System.out.println("------------------" + new Date() + " -------------------------");
		}*/
	}

	boolean _running;
	int _updateInterval;
	File _file;
	long _filePointer;

	public void run() {
		try {
			while (_running) {
				Thread.sleep(_updateInterval);
				long len = _file.length();
				if (len < _filePointer) {
					// Log must have been jibbled or deleted.
					System.out.println(new Date() + "Log file was reset. Restarting logging from start of file.");
					_filePointer = len;
				} else if (len > _filePointer) {
					// File must have had something added to it!
					RandomAccessFile raf = new RandomAccessFile(_file, "r");
					raf.seek(_filePointer);
					String line = null;
					while ((line = raf.readLine()) != null) {
						analyzeLine(line);
					}
					_filePointer = raf.getFilePointer();
					raf.close();
				}
			}
		} catch (Exception e) {
			System.out.println(new Date() + "Fatal error reading log file, log tailing has stopped.");
		}
		// dispose();
	}

	void trace(String outputFile) {
		_file = new File(outputFile);
		_updateInterval = 1000;
		_running = true;
		Thread thread = new Thread(this);
		thread.start();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Reading ELF file " + args[0]);
		System.out.println("Reading log file " + args[1]);
		StackTrace st = new StackTrace();
		st.load(args[0]);
		st.trace(args[1]);
	}

}
