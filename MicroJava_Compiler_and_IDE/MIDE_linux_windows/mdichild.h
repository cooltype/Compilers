#ifndef MDICHILD_H
#define MDICHILD_H

#include <QTextEdit>
#include <QStatusBar>
#include "highlighter.h"

class MdiChild : public QTextEdit
{
    Q_OBJECT

public:
    MdiChild();

    void newFile(QStatusBar *status);
    bool loadFile(const QString &fileName,QStatusBar *status);
    bool save();
    bool saveAs();
    bool saveFile(const QString &fileName);
    QString userFriendlyCurrentFile();
    QString currentFile() { return curFile; }
	QString strippedName(const QString &fullFileName);

protected:
    void closeEvent(QCloseEvent *event);

private slots:
    void documentWasModified();
	void cursorPositionChanged();

private:
    bool maybeSave();
    void setCurrentFile(const QString &fileName);
    

    QString curFile;
	QStatusBar *statusBar;
    bool isUntitled;
	Highlighter *highlighter; 
};

#endif
