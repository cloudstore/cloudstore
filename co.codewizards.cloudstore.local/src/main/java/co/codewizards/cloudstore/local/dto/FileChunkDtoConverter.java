package co.codewizards.cloudstore.local.dto;

import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;
import static java.util.Objects.*;

import co.codewizards.cloudstore.core.dto.FileChunkDto;
import co.codewizards.cloudstore.local.persistence.FileChunk;

public class FileChunkDtoConverter {

	public static FileChunkDtoConverter create() {
		return createObject(FileChunkDtoConverter.class);
	}

	protected FileChunkDtoConverter() { }

	public FileChunkDto toFileChunkDto(final FileChunk fileChunk) {
		requireNonNull(fileChunk, "fileChunk");
		final FileChunkDto fileChunkDto = createObject(FileChunkDto.class);
		fileChunkDto.setOffset(fileChunk.getOffset());
		fileChunkDto.setLength(fileChunk.getLength());
		fileChunkDto.setSha1(fileChunk.getSha1());
		return fileChunkDto;
	}
}
