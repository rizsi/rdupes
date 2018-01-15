package rdupes;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

public class MergeMove extends SimpleFileVisitor<Path>{
	Path src;
	Path tg;
	public MergeMove(Path file, Path tgPath) {
		src=file;
		tg=tgPath;
		tg.toFile().getParentFile().mkdirs();
	}
	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
		Files.delete(dir);
		return super.postVisitDirectory(dir, exc);
	}
	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		Path rel=src.relativize(dir);
		Path ftg=tg.resolve(rel);
		if(!ftg.toFile().exists())
		{
			Files.move(dir, ftg, StandardCopyOption.ATOMIC_MOVE);
			return FileVisitResult.SKIP_SUBTREE;
		}else
		{
			return FileVisitResult.CONTINUE;
		}
	}
	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		Path rel=src.relativize(file);
		Path ftg=tg.resolve(rel);
		ftg.toFile().getParentFile().mkdirs();
		Files.move(file, ftg, StandardCopyOption.ATOMIC_MOVE);
		return super.visitFile(file, attrs);
	}
	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
		return super.visitFileFailed(file, exc);
	}

}
