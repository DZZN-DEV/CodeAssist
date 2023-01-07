package org.jetbrains.kotlin.cli.jvm.compiler.jarfs

import org.jetbrains.kotlin.com.intellij.openapi.util.io.BufferExposingByteArrayInputStream
import org.jetbrains.kotlin.com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.com.intellij.openapi.vfs.VirtualFileSystem
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

internal class FastJarVirtualFile(
    private val handler: FastJarHandler,
    private val name: CharSequence,
    private val length: Int,
    private val parent: FastJarVirtualFile?,
    private val entryDescription: ZipEntryDescription?,
) : VirtualFile() {

    private var myChildrenArray = EMPTY_ARRAY
    private val myChildrenList: MutableList<VirtualFile> = mutableListOf()

    init {
        parent?.myChildrenList?.add(this)
    }

    fun initChildrenArrayFromList() {
        myChildrenArray = myChildrenList.toTypedArray()
        myChildrenList.clear()
    }

    override fun getName(): String {
        return name.toString()
    }

    override fun getNameSequence(): CharSequence {
        return name
    }

    override fun getFileSystem(): VirtualFileSystem {
        return handler.fileSystem
    }

    override fun getPath(): String {
        if (parent == null) {
            return FileUtil.toSystemIndependentName(handler.file.path) + "!/"
        }
        val parentPath = parent.path
        val answer = StringBuilder(parentPath.length + 1 + name.length)
        answer.append(parentPath)
        if (answer[answer.length - 1] != '/') {
            answer.append('/')
        }
        answer.append(name)
        return answer.toString()
    }

    override fun isWritable(): Boolean {
        return false
    }

    override fun isDirectory(): Boolean {
        return length < 0
    }

    override fun isValid(): Boolean {
        return true
    }

    override fun getParent(): VirtualFile? {
        return parent
    }

    override fun getChildren(): Array<VirtualFile> {
        return myChildrenArray
    }

    @Throws(IOException::class)
    override fun getOutputStream(requestor: Any, newModificationStamp: Long, newTimeStamp: Long): OutputStream {
        throw UnsupportedOperationException("JarFileSystem is read-only")
    }

    @Throws(IOException::class)
    override fun contentsToByteArray(): ByteArray {
        if (entryDescription == null) return EMPTY_BYTE_ARRAY
        return handler.contentsToByteArray(entryDescription)
    }

    override fun getTimeStamp(): Long = 0

    override fun getLength(): Long = length.toLong()

    override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {}

    @Throws(IOException::class)
    override fun getInputStream(): InputStream {
        return BufferExposingByteArrayInputStream(contentsToByteArray())
    }

    override fun getModificationStamp(): Long {
        return 0
    }
}

private val EMPTY_BYTE_ARRAY = ByteArray(0)