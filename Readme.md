# JSettlers 1.0

Historical repo for JSettlers 1.0: Converted from CVS to git by Jeremy D Monin.

For the current JSettlers repo, visit https://github.com/jdmonin/JSettlers2 or http://nand.net/jsettlers/

## Branches

- **jsettlers-1-0-branch** holds the released versions 1.0.2 through 1.0.6 (all released in 2004).
  The current JSettlers repo is based on code from this branch.
- **master** has some further 2005 work which never became a release.
- Those two branches diverge June 2004. Their last common commit is:  
  `4c2920c updated for many bug fixes and build changes`

## Tags

JSettlers releases are tagged as `jsettlers-1-0-2` through `jsettlers-1-0-6`.
The conversion process made one-off commits for the 1.0.2 through 1.0.5 tags,
but their parent commits are in `jsettlers-1-0-branch`.

## Project history

- 2000-2002: Robert S Thomas starts the JSettlers project
- 2004-2005: Project moves to https://sourceforge.net/projects/jsettlers/ , starts a CVS repository there,
  releases versions through 1.0.6, then goes dormant
- 2007: Jeremy D Monin adds features and bug fixes; patches posted to sourceforge based on 1.0.6,
  then new 1.1.x releases to http://nand.net/jsettlers/
- 2008: Eli McGowan and Christopher McNeil fork dormant sourceforge project to https://sourceforge.net/projects/jsettlers2/ ,
  starting a *new* CVS jsettlers2 repo based on Jeremy's http://nand.net/jsettlers/ source (version 1.1.04),
  inviting Jeremy to join the project.  
  Jeremy also later joined the original jsettlers project; by that point, merging the repos would be complicated
- 2008-2012: Jeremy releases versions through 1.1.13 at sourceforge
- 2012: Jeremy converts jsettlers2 repo to git, moves project to github
- 2012-present: New versions released at github
- 2020: For historical reference, Jeremy converts original jsettlers cvs repo to git
  published at https://github.com/jdmonin/JSettlers1

## Conversion details

Converted 2020-02-01 using cvs2git (cvs2svn-2.5.0) from http://cvs2svn.tigris.org/cvs2git.html
and config file `cvs2git-jsettlers1.options`

- Get CVS repo from sourceforge
    - Sourceforge project page: https://sourceforge.net/projects/jsettlers/
    - `mkdir cvs-js1-rsync && cd cvs-js1-rsync`
    - `rsync -ai a.cvs.sourceforge.net::cvsroot/jsettlers/ .`
- Get cvs2git and the cvs client
    - Download cvs2svn from http://cvs2svn.tigris.org/files/documents/1462/49543/cvs2svn-2.5.0.tar.gz
    - Get cvs client from your distribution or https://www.nongnu.org/cvs/
- Conversion
    - Change to directory containing cvs2svn
    - Update `cvs2git-jsettlers1.options` with tag/branch strategy, commit-author info, path to downloaded repo cvs-js1-rsync, etc
    - `mkdir cvs2git-tmp`
    - run cvs2git, creating git-dump.dat  
      `./cvs2git --options=cvs2git-jsettlers1.options`
    - make a new empty git repo
      ```
      git init
      git config user.email "jeremy@nand.net"
      git config user.name "Jeremy D Monin"
      ```
    - import into git repo  
      `cat ../cvs2svn-tmp/git-blob.dat ../cvs2svn-tmp/git-dump.dat | git fast-import`
    - ran `contrib/git-move-refs.py` cleanup script; had no effect on release tags
    - examine results: `gitk` or `git log`, `git branch --list`, etc
