package be.limero;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nl.lxtreme.binutils.elf.Attribute;
import nl.lxtreme.binutils.elf.Elf;
import nl.lxtreme.binutils.elf.ProgramHeader;
import nl.lxtreme.binutils.elf.Section;
import nl.lxtreme.binutils.elf.Symbol;

public class ArduinoStackTrace implements Runnable {
	static Logger log = Logger.getLogger(ArduinoStackTrace.class.getName());

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
	/*
	 * Exception (28): epc1=0x40202347 epc2=0x00000000 epc3=0x00000000
	 * excvaddr=0x00000004 depc=0x00000000
	 * 
	 * ctx: cont sp: 3ffefb80 end: 3ffefda0 offset: 01a0
	 * 
	 * >>>stack>>> 3ffefd20: 3fffdad0 0000000c 3ffeed48 3ffeed6c 3ffefd30:
	 * 3fffdad0 3ffe83d5 3fff06fc 402023c0 3ffefd40: 3ffe8300 feefeffe 3ffeed48
	 * 402035d4 3ffefd50: feefeffe feefeffe 3ffeed48 402023e2 3ffefd60: 3fffdad0
	 * 3ffe83d5 3ffeed48 40201c6a 3ffefd70: feefeffe feefeffe feefeffe feefeffe
	 * 3ffefd80: feefeffe 00000000 3ffeed64 40203498 3ffefd90: feefeffe feefeffe
	 * 3ffeed80 40106058 <<<stack<<<
	 * 
	 * ets Jan 8 2013,rst cause:2, boot mode:(3,6)
	 * 
	 * load 0x4010f000, len 1264, room 16
	 */

	void analyzeLine(String line) {
		Pattern pattern = Pattern.compile("([A-Fa-f0-9]+)");
		Matcher matcher = pattern.matcher(line);
		while (matcher.find()) {
			String hex = matcher.group(0);
			long address = Long.parseLong(hex, 16);
			Symbol symbol = _elf.getSymbol(address);
			String type;
			if (address < 0x40000000)
				type = "DATA";
			else
				type = "CODE";
			System.out.printf(" %4s %s +0x%X \n", type, symbol.getName(), address - symbol.getValue());
		}
	}

	boolean _running;
	int _updateInterval;
	File _file;
	long _filePointer;

	public void run() {
		boolean isInsideStack = false;
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
						if (line.contains("<<<stack<<<")) {
							isInsideStack = false;
							log.info("STACK END");
						}
						if (isInsideStack || line.contains("epc1="))
							if ( line.contains("epc1=") ) log.info(" EXCEPTION" );
							analyzeLine(line);
						if (line.contains(">>>stack>>>")) {
							isInsideStack = true;
							log.info("STACK START");
						}
					}
					_filePointer = raf.getFilePointer();
					raf.close();
				}
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "Fatal error reading log file, log tailing has stopped.", e);
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
		ArduinoStackTrace st = new ArduinoStackTrace();
		st.load(args[0]);
		st.trace(args[1]);
	}

}
