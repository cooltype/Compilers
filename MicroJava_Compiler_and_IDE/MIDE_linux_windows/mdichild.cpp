#include <QtGui>
#include "mdichild.h"
#include <iostream>
#include <sstream>

using namespace std;

MdiChild::MdiChild()
{
    setAttribute(Qt::WA_DeleteOnClose);
    isUntitled = true;
	highlighter = new Highlighter(this->document());
	this->setFont(QFont( "verdana", 12, QFont::AnyStyle, TRUE ));
}

void MdiChild::newFile(QStatusBar *status)
{
    static int sequenceNumber = 1;
	
    isUntitled = true;
    curFile = tr("project%1.mj").arg(sequenceNumber++);
    setWindowTitle(curFile + "[*]");
	
    connect(document(), SIGNAL(contentsChanged()),
            this, SLOT(documentWasModified()));


	statusBar = status;
	connect(this, SIGNAL(cursorPositionChanged()),
            this, SLOT(cursorPositionChanged()));


	this->setPlainText("program Project{ \n\n	void main(){\n	\n\n\n	}\n\n\n}"); 
}


bool MdiChild::loadFile(const QString &fileName, QStatusBar *status)
{
    QFile file(fileName);
    if (!file.open(QFile::ReadOnly | QFile::Text)) {
        QMessageBox::warning(this, tr("MDI"),
                             tr("Cannot read file %1:\n%2.")
                             .arg(fileName)
                             .arg(file.errorString()));
        return false;
    }

    QTextStream in(&file);
    QApplication::setOverrideCursor(Qt::WaitCursor);
    setPlainText(in.readAll());
    QApplication::restoreOverrideCursor();

    setCurrentFile(fileName);

    connect(document(), SIGNAL(contentsChanged()),
            this, SLOT(documentWasModified()));


	statusBar = status;
	connect(this, SIGNAL(cursorPositionChanged()),
            this, SLOT(cursorPositionChanged()));

    return true;
}

bool MdiChild::save()
{
    if (isUntitled) {
        return saveAs();
    } else {
        return saveFile(curFile);
    }
}

bool MdiChild::saveAs()
{
    QString fileName =  QFileDialog::getSaveFileName(this, tr("Save As"),
                            curFile,
                            tr("MicroJava (*.mj)"));
    if (fileName.isEmpty())
        return false;

    return saveFile(fileName);
}

bool MdiChild::saveFile(const QString &fileName)
{
    QFile file(fileName);
    if (!file.open(QFile::WriteOnly | QFile::Text)) {
        QMessageBox::warning(this, tr("MDI"),
                             tr("Cannot write file %1:\n%2.")
                             .arg(fileName)
                             .arg(file.errorString()));
		
        return false;
    }
	
	curFile = fileName;

	
    QTextStream out(&file);
    QApplication::setOverrideCursor(Qt::WaitCursor);
    out << toPlainText();
    QApplication::restoreOverrideCursor();

    setCurrentFile(fileName);
    return true;
}

QString MdiChild::userFriendlyCurrentFile()
{
    return strippedName(curFile);
}

void MdiChild::closeEvent(QCloseEvent *event)
{
    if (maybeSave()) {
        event->accept();
    } else {
        event->ignore();
    }
}

void MdiChild::documentWasModified()
{
    setWindowModified(document()->isModified());
}

void MdiChild::cursorPositionChanged()
{
	QTextCursor cursor      = this->textCursor();

	// Current line text
	//QString cur_line_text   = cursor.block().text().trimmed();

	std::ostringstream oss;
	oss <<"Line:"<< cursor.blockNumber()+1 <<"  Colum:"<<cursor.columnNumber();
	statusBar->showMessage( QString::fromStdString(oss.str()));

}


bool MdiChild::maybeSave()
{
    if (document()->isModified()) {
	QMessageBox::StandardButton ret;
        ret = QMessageBox::warning(this, tr("MDI"),
                     tr("'%1' has been modified.\n"
                        "Do you want to save your changes?")
                     .arg(userFriendlyCurrentFile()),
                     QMessageBox::Save | QMessageBox::Discard
		     | QMessageBox::Cancel);
        if (ret == QMessageBox::Save)
            return save();
        else if (ret == QMessageBox::Cancel)
            return false;
    }
    return true;
}

void MdiChild::setCurrentFile(const QString &fileName)
{
    curFile = QFileInfo(fileName).canonicalFilePath();
    isUntitled = false;
    document()->setModified(false);
    setWindowModified(false);
    setWindowTitle(userFriendlyCurrentFile() + "[*]");
}

QString MdiChild::strippedName(const QString &fullFileName)
{
    return QFileInfo(fullFileName).fileName();
}

